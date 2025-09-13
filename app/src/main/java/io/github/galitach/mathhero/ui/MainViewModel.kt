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
import io.github.galitach.mathhero.data.Rank
import io.github.galitach.mathhero.data.SharedPreferencesManager
import java.util.Calendar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SoundEvent {
    object Correct : SoundEvent()
    object Incorrect : SoundEvent()
}

sealed class OneTimeEvent {
    data class LaunchPurchaseFlow(val productDetails: ProductDetails) : OneTimeEvent()
    data class ShowHintDialog(val problem: MathProblem) : OneTimeEvent()
}

data class UiState(
    val problem: MathProblem? = null,
    val isDailyProblemSolved: Boolean = false,
    val showMultipleChoice: Boolean = true,
    val shuffledAnswers: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val isAnswerRevealed: Boolean = false,
    val archivedProblems: List<MathProblem> = emptyList(),
    val isSaveStreakAdLoaded: Boolean = false,
    val showSaveStreakDialog: Boolean = false,
    val streakCount: Int = 0,
    val highestStreakCount: Int = 0,
    val triggerWinAnimation: Boolean = false,
    val triggerRankUpAnimation: Boolean = false,
    val currentRank: Rank? = null,
    val needsDifficultySelection: Boolean = false,
    val showSuggestLowerDifficultyDialog: Boolean = false,
    val difficultyDescription: String? = null,
    val playSoundEvent: SoundEvent? = null,
    val isPro: Boolean = false,
    val showStreakSavedToast: Boolean = false,
    val proProductDetails: ProductDetails? = null
)

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val billingManager: BillingManager
) : AndroidViewModel(application) {

    private val repository: MathProblemRepository
    private var dailyProblemId: Int = -1

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _oneTimeEvent = MutableSharedFlow<OneTimeEvent>()
    val oneTimeEvent = _oneTimeEvent.asSharedFlow()

    init {
        repository = MathProblemRepository(application, SharedPreferencesManager)
        dailyProblemId = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        observeBillingChanges()

        if (!SharedPreferencesManager.isDifficultySet()) {
            _uiState.update { it.copy(needsDifficultySelection = true) }
        } else {
            initializeProblem()
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

    private fun initializeProblem() {
        val dailyProblem = repository.getCurrentProblem()
        val isDailyProblemAlreadySolved = SharedPreferencesManager.getArchivedProblems().any { it.id == dailyProblemId }

        val problemToLoad = if (isDailyProblemAlreadySolved) {
            repository.getBonusProblem()
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

        val highestStreak = SharedPreferencesManager.getHighestStreakCount()
        val difficultySettings = SharedPreferencesManager.getDifficultySettings()

        _uiState.value = _uiState.value.copy(
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

    fun onDifficultySelected(settings: DifficultySettings) {
        SharedPreferencesManager.saveDifficultySettings(settings)
        _uiState.update {
            it.copy(
                needsDifficultySelection = false,
                difficultyDescription = generateDifficultyDescription(settings)
            )
        }
        initializeProblem()
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

    fun onConfirmAnswerClicked() {
        val state = _uiState.value
        val problem = state.problem
        if (state.isAnswerRevealed || state.selectedAnswer == null || problem == null) return

        val isCorrect = state.selectedAnswer == problem.answer
        SharedPreferencesManager.addProblemToArchive(problem)
        SharedPreferencesManager.logProblemResult(problem, isCorrect)

        _uiState.update { it.copy(isAnswerRevealed = true, archivedProblems = repository.getArchivedProblems()) }
        savedStateHandle[KEY_IS_ANSWER_REVEALED] = true

        if (isCorrect) {
            handleCorrectAnswer()
        } else {
            handleIncorrectAnswer()
        }
    }

    private fun handleCorrectAnswer() {
        SharedPreferencesManager.resetConsecutiveWrongAnswers()
        val oldHighestStreak = SharedPreferencesManager.getHighestStreakCount()
        val oldRank = Rank.getRankForStreak(oldHighestStreak)

        SharedPreferencesManager.updateStreak(true)

        val newHighestStreak = SharedPreferencesManager.getHighestStreakCount()
        val newRank = Rank.getRankForStreak(newHighestStreak)
        val hasRankedUp = newRank.level > oldRank.level

        val problemId = _uiState.value.problem?.id
        val isDailyProblemNowSolved = problemId == dailyProblemId || _uiState.value.isDailyProblemSolved

        _uiState.update {
            it.copy(
                streakCount = SharedPreferencesManager.getStreakCount(),
                highestStreakCount = newHighestStreak,
                triggerWinAnimation = !hasRankedUp,
                triggerRankUpAnimation = hasRankedUp,
                currentRank = newRank,
                playSoundEvent = SoundEvent.Correct,
                isDailyProblemSolved = isDailyProblemNowSolved
            )
        }
    }

    private fun handleIncorrectAnswer() {
        SharedPreferencesManager.incrementConsecutiveWrongAnswers()
        _uiState.update { it.copy(playSoundEvent = SoundEvent.Incorrect) }

        if (uiState.value.streakCount > 0) {
            if (uiState.value.isPro) {
                _uiState.update { it.copy(showStreakSavedToast = true) }
                onStreakSaveCompleted()
            } else {
                _uiState.update { it.copy(showSaveStreakDialog = true) }
            }
        } else {
            val consecutiveWrong = SharedPreferencesManager.getConsecutiveWrongAnswers()
            val shouldSuggestLowering = consecutiveWrong >= 3
            _uiState.update { it.copy(showSuggestLowerDifficultyDialog = shouldSuggestLowering) }
        }
    }

    fun onStreakResetConfirmed() {
        SharedPreferencesManager.updateStreak(false)
        val consecutiveWrong = SharedPreferencesManager.getConsecutiveWrongAnswers()
        val shouldSuggestLowering = consecutiveWrong >= 3

        _uiState.update {
            it.copy(
                streakCount = SharedPreferencesManager.getStreakCount(),
                showSaveStreakDialog = false,
                showSuggestLowerDifficultyDialog = shouldSuggestLowering
            )
        }
    }

    fun onSuggestLowerDifficultyDismissed() {
        SharedPreferencesManager.resetConsecutiveWrongAnswers()
        _uiState.update { it.copy(showSuggestLowerDifficultyDialog = false) }
    }

    fun onStreakSavedByAd() {
        onStreakSaveCompleted()
    }

    private fun onStreakSaveCompleted() {
        SharedPreferencesManager.resetConsecutiveWrongAnswers()
        _uiState.update { it.copy(showSaveStreakDialog = false) }
    }

    fun onNextProblemRequested() {
        loadBonusProblem()
    }

    private fun loadBonusProblem() {
        val bonusProblem = repository.getBonusProblem()
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
                    triggerWinAnimation = false,
                    triggerRankUpAnimation = false
                )
            }
            savedStateHandle.keys().forEach { key -> savedStateHandle.remove<Any>(key) }
            savedStateHandle[KEY_SHUFFLED_ANSWERS] = shuffledAnswers
        }
    }

    fun onWinAnimationComplete() {
        _uiState.update { it.copy(triggerWinAnimation = false) }
    }

    fun onRankUpAnimationComplete() {
        _uiState.update { it.copy(triggerRankUpAnimation = false) }
    }

    fun onSoundEventHandled() {
        _uiState.update { it.copy(playSoundEvent = null) }
    }

    fun setSaveStreakAdLoaded(isLoaded: Boolean) {
        _uiState.update { it.copy(isSaveStreakAdLoaded = isLoaded) }
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

    companion object {
        private const val KEY_SHUFFLED_ANSWERS = "shuffled_answers"
        private const val KEY_SELECTED_ANSWER = "selected_answer"
        private const val KEY_IS_ANSWER_REVEALED = "is_answer_revealed"
    }
}