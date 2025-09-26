package io.github.galitach.mathhero.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.billing.BillingManager
import io.github.galitach.mathhero.data.DifficultyLevel
import io.github.galitach.mathhero.data.DifficultySettings
import io.github.galitach.mathhero.data.MathProblem
import io.github.galitach.mathhero.data.MathProblemRepository
import io.github.galitach.mathhero.data.ProblemResult
import io.github.galitach.mathhero.data.ProgressRepository
import io.github.galitach.mathhero.data.Rank
import io.github.galitach.mathhero.data.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SoundEvent {
    object Correct : SoundEvent()
    object Incorrect : SoundEvent()
}

sealed class OneTimeEvent {
    data class LaunchPurchaseFlow(val productDetails: ProductDetails) : OneTimeEvent()
    data class ShowHintDialog(val problem: MathProblem) : OneTimeEvent()
    data class ShowSaveStreakDialog(val brokenStreakValue: Int) : OneTimeEvent()
    object ShowSuggestLowerDifficultyDialog : OneTimeEvent()
    object TriggerWinAnimation : OneTimeEvent()
    object TriggerRankUpAnimation : OneTimeEvent()
    object KidModeSessionFinished : OneTimeEvent()
}

enum class AdLoadState {
    IDLE, LOADING, LOADED, FAILED
}

data class UiState(
    val problem: MathProblem? = null,
    val isDailyProblemSolved: Boolean = false,
    val showMultipleChoice: Boolean = true,
    val shuffledAnswers: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val isAnswerRevealed: Boolean = false,
    val archivedProblems: List<MathProblem> = emptyList(),
    val saveStreakAdState: AdLoadState = AdLoadState.IDLE,
    val streakCount: Int = 0,
    val highestStreakCount: Int = 0,
    val currentRank: Rank? = null,
    val needsDifficultySelection: Boolean = false,
    val difficultyDescription: String? = null,
    val playSoundEvent: SoundEvent? = null,
    val isPro: Boolean = false,
    val showStreakSavedToast: Boolean = false,
    val proProductDetails: ProductDetails? = null,
    val isLoadingNextProblem: Boolean = false,
    // Kid Mode State
    val isKidModeActive: Boolean = false,
    val kidModeTargetCorrectAnswers: Int = 0,
    val kidModeSessionCorrectAnswers: Int = 0
)

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val billingManager: BillingManager,
    private val progressRepository: ProgressRepository
) : AndroidViewModel(application) {

    private val repository: MathProblemRepository

    private val _uiState = MutableStateFlow(
        UiState(
            isKidModeActive = savedStateHandle.get<Boolean>(KEY_KID_MODE_ACTIVE) ?: false,
            kidModeTargetCorrectAnswers = savedStateHandle.get<Int>(KEY_KID_MODE_TARGET) ?: 0,
            kidModeSessionCorrectAnswers = savedStateHandle.get<Int>(KEY_KID_MODE_PROGRESS) ?: 0
        )
    )
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _oneTimeEvent = MutableSharedFlow<OneTimeEvent>()
    val oneTimeEvent = _oneTimeEvent.asSharedFlow()

    init {
        repository = MathProblemRepository(application, SharedPreferencesManager)

        val needsSelection = !SharedPreferencesManager.isDifficultySet()
        _uiState.update { it.copy(needsDifficultySelection = needsSelection) }

        observeBillingChanges()

        if (!needsSelection && !uiState.value.isKidModeActive) {
            initializeProblem()
        } else if (uiState.value.isKidModeActive) {
            // If Kid Mode was active before process death, restore the problem
            val restoredProblem = savedStateHandle.get<MathProblem>(KEY_PROBLEM)
            if (restoredProblem != null) {
                loadInitialState(restoredProblem, isDailySolved = false, clearSavedState = false)
            } else {
                // This case is unlikely but as a fallback, start a new problem for the session
                onKidModeSetup(uiState.value.kidModeTargetCorrectAnswers, SharedPreferencesManager.getDifficultySettings())
            }
        }
    }

    private fun observeBillingChanges() {
        billingManager.isPro
            .onEach { isPro ->
                _uiState.update { it.copy(isPro = isPro) }
                if (isPro) {
                    SharedPreferencesManager.setProUser(true)
                }
            }
            .launchIn(viewModelScope)

        billingManager.productDetails
            .onEach { details ->
                _uiState.update { it.copy(proProductDetails = details) }
            }
            .launchIn(viewModelScope)
    }

    private fun initializeProblem() = viewModelScope.launch {
        // Restore problem from saved state if available (process death)
        val restoredProblem = savedStateHandle.get<MathProblem>(KEY_PROBLEM)
        if (restoredProblem != null) {
            loadInitialState(restoredProblem, isDailySolved = false, clearSavedState = false)
            return@launch
        }

        // Otherwise, generate a new problem
        val dailyProblem = withContext(Dispatchers.Default) { repository.getCurrentProblem() }
        val isDailyProblemAlreadySolved = SharedPreferencesManager.getArchivedProblems().any { it.id == dailyProblem.id }

        val problemToLoad = if (isDailyProblemAlreadySolved) {
            withContext(Dispatchers.Default) { repository.getBonusProblem() }
        } else {
            dailyProblem
        }

        loadInitialState(problemToLoad, isDailyProblemAlreadySolved, clearSavedState = isDailyProblemAlreadySolved)
    }

    private fun loadInitialState(problem: MathProblem?, isDailySolved: Boolean, clearSavedState: Boolean) {
        if (clearSavedState) {
            savedStateHandle.keys().forEach { key -> savedStateHandle.remove<Any>(key) }
        }

        val shuffledAnswers = savedStateHandle.get<List<String>>(KEY_SHUFFLED_ANSWERS)
            ?: problem?.let { listOf(it.answer, it.distractor1, it.distractor2).shuffled() }
            ?: emptyList()

        if (!savedStateHandle.contains(KEY_SHUFFLED_ANSWERS)) {
            savedStateHandle[KEY_SHUFFLED_ANSWERS] = shuffledAnswers
        }
        if (problem != null) {
            savedStateHandle[KEY_PROBLEM] = problem
        }

        val highestStreak = SharedPreferencesManager.getHighestStreakCount()
        val difficultySettings = SharedPreferencesManager.getDifficultySettings()

        _uiState.update {
            it.copy(
                problem = problem,
                isDailyProblemSolved = isDailySolved,
                shuffledAnswers = shuffledAnswers,
                selectedAnswer = savedStateHandle.get<String>(KEY_SELECTED_ANSWER),
                isAnswerRevealed = savedStateHandle.get<Boolean>(KEY_IS_ANSWER_REVEALED) ?: false,
                archivedProblems = repository.getArchivedProblems(),
                streakCount = SharedPreferencesManager.getStreakCount(),
                highestStreakCount = highestStreak,
                currentRank = Rank.getRankForStreak(highestStreak),
                difficultyDescription = generateDifficultyDescription(difficultySettings)
            )
        }
    }

    fun onDifficultySelected(settings: DifficultySettings) {
        SharedPreferencesManager.saveDifficultySettings(settings)
        _uiState.update {
            it.copy(
                needsDifficultySelection = false,
                difficultyDescription = generateDifficultyDescription(settings)
            )
        }
        viewModelScope.launch {
            val newProblem = withContext(Dispatchers.Default) { repository.getCurrentProblem() }
            loadNewProblem(newProblem)
        }
    }

    private fun generateDifficultyDescription(settings: DifficultySettings): String {
        val matchingLevel = DifficultyLevel.entries.find { it.settings == settings }
        if (matchingLevel != null) {
            return getApplication<Application>().getString(matchingLevel.titleRes)
        }

        val ops = settings.operations.sortedBy { it.ordinal }.joinToString(", ") { it.symbol }
        return getApplication<Application>().getString(R.string.custom_difficulty_desc, ops, settings.maxNumber)
    }

    fun onHintClicked() {
        viewModelScope.launch {
            _uiState.value.problem?.let {
                _oneTimeEvent.emit(OneTimeEvent.ShowHintDialog(it))
            }
        }
    }

    fun onMultipleChoiceAnswerSelected(answer: String) {
        if (_uiState.value.isAnswerRevealed) return
        _uiState.update { it.copy(selectedAnswer = answer) }
        savedStateHandle[KEY_SELECTED_ANSWER] = answer
    }

    fun onAnswerSelectionCleared() {
        if (_uiState.value.isAnswerRevealed) return
        _uiState.update { it.copy(selectedAnswer = null) }
        savedStateHandle[KEY_SELECTED_ANSWER] = null
    }

    fun onConfirmAnswerClicked() = viewModelScope.launch {
        val state = _uiState.value
        val problem = state.problem
        if (state.isAnswerRevealed || state.selectedAnswer == null || problem == null) return@launch

        val isCorrect = state.selectedAnswer == problem.answer
        if (!state.isKidModeActive) {
            SharedPreferencesManager.addProblemToArchive(problem)
        }
        // Always log progress, even in Kid Mode
        progressRepository.logProblemResult(problem, isCorrect)


        savedStateHandle[KEY_IS_ANSWER_REVEALED] = true

        val dailyProblemId = withContext(Dispatchers.Default) { repository.getCurrentProblem().id }
        val wasDailyProblem = problem.id == dailyProblemId

        if (isCorrect) {
            if (state.isKidModeActive) {
                incrementKidModeProgress()
            } else {
                handleCorrectAnswer(wasDailyProblem)
            }
        } else {
            handleIncorrectAnswer(wasDailyProblem)
        }
    }

    private fun handleCorrectAnswer(isDailyProblem: Boolean) {
        SharedPreferencesManager.resetConsecutiveWrongAnswers()
        val oldHighestStreak = SharedPreferencesManager.getHighestStreakCount()
        val oldRank = Rank.getRankForStreak(oldHighestStreak)

        SharedPreferencesManager.updateStreak(true)

        val newHighestStreak = SharedPreferencesManager.getHighestStreakCount()
        val newRank = Rank.getRankForStreak(newHighestStreak)
        val hasRankedUp = newRank.level > oldRank.level

        viewModelScope.launch {
            if (hasRankedUp) {
                _oneTimeEvent.emit(OneTimeEvent.TriggerRankUpAnimation)
            } else {
                _oneTimeEvent.emit(OneTimeEvent.TriggerWinAnimation)
            }
        }

        _uiState.update {
            it.copy(
                isAnswerRevealed = true,
                isDailyProblemSolved = it.isDailyProblemSolved || isDailyProblem,
                archivedProblems = repository.getArchivedProblems(),
                streakCount = SharedPreferencesManager.getStreakCount(),
                highestStreakCount = newHighestStreak,
                currentRank = newRank,
                playSoundEvent = SoundEvent.Correct,
            )
        }
    }

    private fun handleIncorrectAnswer(isDailyProblem: Boolean) {
        if (!uiState.value.isKidModeActive) {
            SharedPreferencesManager.incrementConsecutiveWrongAnswers()
        }
        val streakBeforeReset = _uiState.value.streakCount
        val isPro = _uiState.value.isPro

        _uiState.update {
            it.copy(
                isAnswerRevealed = true,
                isDailyProblemSolved = it.isDailyProblemSolved || isDailyProblem,
                archivedProblems = repository.getArchivedProblems(),
                playSoundEvent = SoundEvent.Incorrect
            )
        }

        if (streakBeforeReset > 0 && !uiState.value.isKidModeActive) {
            if (isPro) {
                onStreakSaveCompleted(streakBeforeReset)
            } else {
                viewModelScope.launch {
                    _oneTimeEvent.emit(OneTimeEvent.ShowSaveStreakDialog(streakBeforeReset))
                }
            }
        } else if (!uiState.value.isKidModeActive) {
            checkAndSuggestLowerDifficulty()
        }
    }

    private fun checkAndSuggestLowerDifficulty() {
        val consecutiveWrong = SharedPreferencesManager.getConsecutiveWrongAnswers()
        if (consecutiveWrong >= 3) {
            viewModelScope.launch {
                _oneTimeEvent.emit(OneTimeEvent.ShowSuggestLowerDifficultyDialog)
            }
        }
    }

    fun onStreakResetConfirmed() {
        SharedPreferencesManager.updateStreak(false)
        _uiState.update { it.copy(streakCount = 0) }
        checkAndSuggestLowerDifficulty()
        _uiState.update { it.copy(saveStreakAdState = AdLoadState.IDLE) }
    }

    fun onDifficultyChangeFromSuggestion() {
        SharedPreferencesManager.resetConsecutiveWrongAnswers()
    }

    fun onSuggestLowerDifficultyDismissed() {}

    fun onStreakSaveCompleted(restoredStreak: Int) {
        SharedPreferencesManager.setStreakCount(restoredStreak)
        checkAndSuggestLowerDifficulty()
        _uiState.update {
            it.copy(
                streakCount = restoredStreak,
                showStreakSavedToast = uiState.value.isPro,
                saveStreakAdState = AdLoadState.IDLE
            )
        }
    }

    fun onNextProblemRequested() {
        if (_uiState.value.isLoadingNextProblem) return
        _uiState.update { it.copy(isLoadingNextProblem = true) }
        viewModelScope.launch {
            if (uiState.value.isKidModeActive) {
                val problem = withContext(Dispatchers.Default) {
                    repository.getBonusProblem()
                }
                loadNewProblem(problem)
            } else {
                loadBonusProblem()
            }
        }
    }

    private suspend fun loadBonusProblem() {
        val bonusProblem = withContext(Dispatchers.Default) {
            repository.getBonusProblem()
        }
        loadNewProblem(bonusProblem)
    }

    private fun loadNewProblem(problem: MathProblem?) {
        problem?.let {
            val shuffledAnswers = listOf(it.answer, it.distractor1, it.distractor2).shuffled()
            _uiState.update {
                it.copy(
                    problem = problem,
                    shuffledAnswers = shuffledAnswers,
                    selectedAnswer = null,
                    isAnswerRevealed = false,
                    isLoadingNextProblem = false
                )
            }
            savedStateHandle[KEY_SHUFFLED_ANSWERS] = shuffledAnswers
            savedStateHandle[KEY_PROBLEM] = problem
        }
    }

    fun onSoundEventHandled() {
        _uiState.update { it.copy(playSoundEvent = null) }
    }

    fun setSaveStreakAdState(newState: AdLoadState) {
        _uiState.update { it.copy(saveStreakAdState = newState) }
    }

    fun onStreakSaveToastShown() {
        _uiState.update { it.copy(showStreakSavedToast = false) }
    }

    fun initiatePurchaseFlow() {
        viewModelScope.launch {
            _uiState.value.proProductDetails?.let {
                _oneTimeEvent.emit(OneTimeEvent.LaunchPurchaseFlow(it))
            }
        }
    }

    suspend fun getProgressResults(): List<ProblemResult> {
        return progressRepository.getProgressHistory()
    }

    fun onRecommendationDismissed(id: String) {
        SharedPreferencesManager.dismissRecommendation(id)
    }

    // --- KID MODE ---
    fun onKidModeSetup(target: Int, difficulty: DifficultySettings) {
        SharedPreferencesManager.saveDifficultySettings(difficulty) // Save for this session
        savedStateHandle[KEY_KID_MODE_ACTIVE] = true
        savedStateHandle[KEY_KID_MODE_TARGET] = target
        savedStateHandle[KEY_KID_MODE_PROGRESS] = 0
        _uiState.update {
            it.copy(
                isKidModeActive = true,
                kidModeTargetCorrectAnswers = target,
                kidModeSessionCorrectAnswers = 0,
                difficultyDescription = generateDifficultyDescription(difficulty)
            )
        }
        onNextProblemRequested() // Start with a fresh problem for the session
    }

    private fun incrementKidModeProgress() {
        val newProgress = uiState.value.kidModeSessionCorrectAnswers + 1
        savedStateHandle[KEY_KID_MODE_PROGRESS] = newProgress
        _uiState.update {
            it.copy(
                isAnswerRevealed = true,
                kidModeSessionCorrectAnswers = newProgress,
                playSoundEvent = SoundEvent.Correct
            )
        }

        if (newProgress >= uiState.value.kidModeTargetCorrectAnswers) {
            viewModelScope.launch {
                _oneTimeEvent.emit(OneTimeEvent.KidModeSessionFinished)
                onKidModeExited()
            }
        }
    }

    fun onKidModeExited() {
        savedStateHandle[KEY_KID_MODE_ACTIVE] = false
        savedStateHandle[KEY_KID_MODE_TARGET] = 0
        savedStateHandle[KEY_KID_MODE_PROGRESS] = 0
        _uiState.update {
            it.copy(
                isKidModeActive = false,
                kidModeTargetCorrectAnswers = 0,
                kidModeSessionCorrectAnswers = 0
            )
        }
        // Reload the original daily problem
        initializeProblem()
    }

    companion object {
        private const val KEY_PROBLEM = "problem"
        private const val KEY_SHUFFLED_ANSWERS = "shuffled_answers"
        private const val KEY_SELECTED_ANSWER = "selected_answer"
        private const val KEY_IS_ANSWER_REVEALED = "is_answer_revealed"
        private const val KEY_KID_MODE_ACTIVE = "kid_mode_active"
        private const val KEY_KID_MODE_TARGET = "kid_mode_target"
        private const val KEY_KID_MODE_PROGRESS = "kid_mode_progress"
    }
}