package io.github.galitach.mathhero.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object SharedPreferencesManager {

    private const val PREFS_NAME = "MathHeroPrefs"
    private const val KEY_ARCHIVED_PROBLEMS = "archived_math_problems"
    private const val KEY_STREAK_COUNT = "streak_count"
    private const val KEY_HIGHEST_STREAK_COUNT = "highest_streak_count"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_DIFFICULTY_OPERATIONS = "difficulty_operations"
    private const val KEY_DIFFICULTY_MAX_NUMBER = "difficulty_max_number"
    private const val KEY_CONSECUTIVE_WRONG_ANSWERS = "consecutive_wrong_answers"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_SOUND_ENABLED = "sound_enabled"
    private const val KEY_IS_PRO_USER = "is_pro_user"
    private const val KEY_PROGRESS_DATA_MIGRATED = "progress_data_migrated"
    private const val KEY_DISMISSED_RECOMMENDATIONS = "dismissed_recommendations"
    private const val KEY_SUGGEST_DIFFICULTY_ENABLED = "suggest_difficulty_enabled"
    private const val MAX_ARCHIVE_SIZE = 7

    // Legacy key, for migration only
    private const val KEY_PROGRESS_DATA_LEGACY = "progress_data"


    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isSoundEnabled(): Boolean {
        return prefs.getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND_ENABLED, enabled) }
    }

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted() {
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, true) }
    }

    fun getArchivedProblems(): List<MathProblem> {
        val problemStrings = prefs.getStringSet(KEY_ARCHIVED_PROBLEMS, emptySet()) ?: emptySet()
        return problemStrings.mapNotNull { deserializeProblem(it) }.sortedByDescending { it.id }
    }

    fun addProblemToArchive(problem: MathProblem) {
        val currentProblems = getArchivedProblems().toMutableList()
        if (currentProblems.any { it.id == problem.id }) return

        currentProblems.add(0, problem)
        val updatedProblems = currentProblems.take(MAX_ARCHIVE_SIZE)
        val problemStrings = updatedProblems.map { serializeProblem(it) }.toSet()
        prefs.edit { putStringSet(KEY_ARCHIVED_PROBLEMS, problemStrings) }
    }

    private fun serializeProblem(problem: MathProblem): String {
        return with(problem) {
            "$id|$question|$answer|$distractor1|$distractor2|$difficulty|${explanation.orEmpty()}|$num1|$num2|$operator"
        }
    }

    private fun deserializeProblem(problemString: String): MathProblem? {
        return try {
            val parts = problemString.split('|')
            MathProblem(
                id = parts[0].toInt(),
                question = parts[1],
                answer = parts[2],
                distractor1 = parts[3],
                distractor2 = parts[4],
                difficulty = parts[5].toInt(),
                explanation = parts[6].takeIf { it.isNotEmpty() },
                num1 = parts[7].toInt(),
                num2 = parts[8].toInt(),
                operator = parts[9]
            )
        } catch (_: Exception) {
            null
        }
    }

    fun getStreakCount(): Int {
        return prefs.getInt(KEY_STREAK_COUNT, 0)
    }

    fun setStreakCount(count: Int) {
        prefs.edit { putInt(KEY_STREAK_COUNT, count) }
        if (count > getHighestStreakCount()) {
            prefs.edit { putInt(KEY_HIGHEST_STREAK_COUNT, count) }
        }
    }

    fun getHighestStreakCount(): Int {
        return prefs.getInt(KEY_HIGHEST_STREAK_COUNT, 0)
    }

    fun updateStreak(isCorrect: Boolean) {
        val currentStreak = getStreakCount()
        val newStreak = if (isCorrect) currentStreak + 1 else 0

        if (newStreak > getHighestStreakCount()) {
            prefs.edit { putInt(KEY_HIGHEST_STREAK_COUNT, newStreak) }
        }
        prefs.edit { putInt(KEY_STREAK_COUNT, newStreak) }
    }

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false) // Default to false
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }

    fun isDifficultySet(): Boolean {
        return prefs.contains(KEY_DIFFICULTY_MAX_NUMBER)
    }

    fun saveDifficultySettings(settings: DifficultySettings) {
        val operationNames = settings.operations.map { it.name }.toSet()
        prefs.edit {
            putStringSet(KEY_DIFFICULTY_OPERATIONS, operationNames)
            putInt(KEY_DIFFICULTY_MAX_NUMBER, settings.maxNumber)
        }
    }

    fun getDifficultySettings(): DifficultySettings {
        val defaultSettings = DifficultyLevel.NOVICE.settings
        val operationNames = prefs.getStringSet(KEY_DIFFICULTY_OPERATIONS, null)
        val maxNumber = prefs.getInt(KEY_DIFFICULTY_MAX_NUMBER, -1)

        if (operationNames == null || maxNumber == -1) {
            return defaultSettings
        }

        val operations = operationNames.mapNotNull {
            try {
                Operation.valueOf(it)
            } catch (_: IllegalArgumentException) {
                null
            }
        }.toSet()

        return DifficultySettings(operations, maxNumber)
    }

    fun getConsecutiveWrongAnswers(): Int {
        return prefs.getInt(KEY_CONSECUTIVE_WRONG_ANSWERS, 0)
    }

    fun incrementConsecutiveWrongAnswers() {
        val current = getConsecutiveWrongAnswers()
        prefs.edit { putInt(KEY_CONSECUTIVE_WRONG_ANSWERS, current + 1) }
    }

    fun resetConsecutiveWrongAnswers() {
        prefs.edit { putInt(KEY_CONSECUTIVE_WRONG_ANSWERS, 0) }
    }

    fun isProUser(): Boolean {
        return prefs.getBoolean(KEY_IS_PRO_USER, false)
    }

    fun setProUser(isPro: Boolean) {
        prefs.edit { putBoolean(KEY_IS_PRO_USER, isPro) }
    }

    fun getDismissedRecommendationIds(): Set<String> {
        return prefs.getStringSet(KEY_DISMISSED_RECOMMENDATIONS, emptySet()) ?: emptySet()
    }

    fun dismissRecommendation(id: String) {
        val currentDismissed = getDismissedRecommendationIds().toMutableSet()
        currentDismissed.add(id)
        prefs.edit {
            putStringSet(KEY_DISMISSED_RECOMMENDATIONS, currentDismissed)
        }
    }

    fun isSuggestDifficultyEnabled(): Boolean {
        return prefs.getBoolean(KEY_SUGGEST_DIFFICULTY_ENABLED, true)
    }

    fun setSuggestDifficultyEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SUGGEST_DIFFICULTY_ENABLED, enabled) }
    }

    // --- MIGRATION LOGIC ---
    suspend fun migrateProgressDataIfNeeded(repository: ProgressRepository) {
        if (prefs.getBoolean(KEY_PROGRESS_DATA_MIGRATED, false)) {
            return
        }

        val dataStrings = prefs.getStringSet(KEY_PROGRESS_DATA_LEGACY, null) ?: return
        val legacyProblems = dataStrings.mapNotNull { deserializeProgressLegacy(it) }

        legacyProblems.forEach { result ->
            val problem = MathProblem(
                id = 0, question = "", answer = result.answer.toString(), distractor1 = "", distractor2 = "",
                difficulty = 0, explanation = null, num1 = result.num1, num2 = result.num2,
                operator = result.operation.symbol
            )
            repository.logProblemResult(problem, result.wasCorrect)
        }

        prefs.edit {
            remove(KEY_PROGRESS_DATA_LEGACY)
            putBoolean(KEY_PROGRESS_DATA_MIGRATED, true)
        }
    }

    private fun deserializeProgressLegacy(dataString: String): ProblemResult? {
        return try {
            val parts = dataString.split('|')
            ProblemResult(
                timestamp = parts[0].toLong(),
                operation = Operation.valueOf(parts[1]),
                wasCorrect = parts[2].toBoolean(),
                num1 = parts.getOrNull(3)?.toInt() ?: 0,
                num2 = parts.getOrNull(4)?.toInt() ?: 0,
                answer = parts.getOrNull(5)?.toInt() ?: 0
            )
        } catch (_: Exception) {
            null
        }
    }
}