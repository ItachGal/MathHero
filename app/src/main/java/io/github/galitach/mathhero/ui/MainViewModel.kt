package io.github.galitach.mathhero.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import io.github.galitach.mathhero.data.MathProblem
import io.github.galitach.mathhero.data.MathProblemRepository
import io.github.galitach.mathhero.data.Rank
import io.github.galitach.mathhero.data.SharedPreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val problem: MathProblem? = null,
    val showMultipleChoice: Boolean = true,
    val shuffledAnswers: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val isAnswerRevealed: Boolean = false,
    val archivedProblems: List<MathProblem> = emptyList(),
    val isBonusRewardedAdLoaded: Boolean = false,
    val isSaveStreakAdLoaded: Boolean = false,
    val showSaveStreakDialog: Boolean = false,
    val streakCount: Int = 0,
    val highestStreakCount: Int = 0,
    val triggerWinAnimation: Boolean = false,
    val triggerRankUpAnimation: Boolean = false,
    val bonusProblemsRemaining: Int = 0,
    val showVisualHint: Boolean = false,
    val currentRank: Rank? = null
)

class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository: MathProblemRepository

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        repository = MathProblemRepository(application, SharedPreferencesManager)
        initializeProblem()
    }

    private fun initializeProblem() {
        val dailyProblem = repository.getCurrentProblem()
        val isDailyProblemSolved = SharedPreferencesManager.getArchivedProblems().any { it.id == dailyProblem.id }

        val problemToLoad = if (isDailyProblemSolved) {
            repository.getBonusProblem()
        } else {
            dailyProblem
        }

        loadInitialState(problemToLoad, clearSavedState = isDailyProblemSolved)
    }

    private fun loadInitialState(problem: MathProblem?, clearSavedState: Boolean) {
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

        _uiState.value = UiState(
            problem = problem,
            shuffledAnswers = shuffledAnswers,
            selectedAnswer = savedStateHandle.get<String>(KEY_SELECTED_ANSWER),
            isAnswerRevealed = savedStateHandle.get<Boolean>(KEY_IS_ANSWER_REVEALED) ?: false,
            archivedProblems = repository.getArchivedProblems(),
            streakCount = SharedPreferencesManager.getStreakCount(),
            highestStreakCount = highestStreak,
            bonusProblemsRemaining = SharedPreferencesManager.getBonusProblemsRemaining(),
            showVisualHint = savedStateHandle.get<Boolean>(KEY_SHOW_HINT) ?: false,
            currentRank = Rank.getRankForStreak(highestStreak)
        )
    }

    fun onHintClicked() {
        _uiState.update { it.copy(showVisualHint = true) }
        savedStateHandle[KEY_SHOW_HINT] = true
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

        _uiState.update { it.copy(isAnswerRevealed = true, archivedProblems = repository.getArchivedProblems()) }
        savedStateHandle[KEY_IS_ANSWER_REVEALED] = true

        if (isCorrect) {
            handleCorrectAnswer()
        } else {
            handleIncorrectAnswer()
        }
    }

    private fun handleCorrectAnswer() {
        val oldHighestStreak = SharedPreferencesManager.getHighestStreakCount()
        val oldRank = Rank.getRankForStreak(oldHighestStreak)

        SharedPreferencesManager.updateStreak(true)

        val newHighestStreak = SharedPreferencesManager.getHighestStreakCount()
        val newRank = Rank.getRankForStreak(newHighestStreak)
        val hasRankedUp = newRank.level > oldRank.level

        _uiState.update {
            it.copy(
                streakCount = SharedPreferencesManager.getStreakCount(),
                highestStreakCount = newHighestStreak,
                triggerWinAnimation = !hasRankedUp,
                triggerRankUpAnimation = hasRankedUp,
                currentRank = newRank
            )
        }
    }

    private fun handleIncorrectAnswer() {
        _uiState.update { it.copy(showSaveStreakDialog = true) }
    }

    fun onStreakResetConfirmed() {
        SharedPreferencesManager.updateStreak(false)
        _uiState.update {
            it.copy(
                streakCount = SharedPreferencesManager.getStreakCount(),
                showSaveStreakDialog = false
            )
        }
    }

    fun onStreakSavedWithAd() {
        _uiState.update { it.copy(showSaveStreakDialog = false) }
    }

    fun onBonusProblemRequested() {
        if (SharedPreferencesManager.getBonusProblemsRemaining() > 0) {
            SharedPreferencesManager.useBonusProblem()
            loadBonusProblem()
        }
    }

    fun onBonusAdRewardEarned() {
        SharedPreferencesManager.addBonusProblemsFromAd()
        SharedPreferencesManager.useBonusProblem()
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
                    triggerRankUpAnimation = false,
                    bonusProblemsRemaining = SharedPreferencesManager.getBonusProblemsRemaining(),
                    showVisualHint = false
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

    fun setBonusRewardedAdLoaded(isLoaded: Boolean) {
        _uiState.update { it.copy(isBonusRewardedAdLoaded = isLoaded) }
    }

    fun setSaveStreakAdLoaded(isLoaded: Boolean) {
        _uiState.update { it.copy(isSaveStreakAdLoaded = isLoaded) }
    }

    companion object {
        private const val KEY_SHUFFLED_ANSWERS = "shuffled_answers"
        private const val KEY_SELECTED_ANSWER = "selected_answer"
        private const val KEY_IS_ANSWER_REVEALED = "is_answer_revealed"
        private const val KEY_SHOW_HINT = "show_hint"
    }
}