package io.github.galitach.mathhero.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.Calendar

object SharedPreferencesManager {

    private const val PREFS_NAME = "MathHeroPrefs"
    private const val KEY_ARCHIVED_PROBLEMS = "archived_math_problems"
    private const val KEY_STREAK_COUNT = "streak_count"
    private const val KEY_HIGHEST_STREAK_COUNT = "highest_streak_count"
    private const val KEY_FREE_BONUS_USED_COUNT = "free_bonus_used_count"
    private const val KEY_LAST_BONUS_DAY = "last_bonus_day"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val MAX_ARCHIVE_SIZE = 7
    private const val MAX_FREE_BONUS_RIDDLES = 3

    private lateinit var prefs: SharedPreferences

    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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

    private fun getFreeBonusRiddlesUsedToday(): Int {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastBonusDay = prefs.getInt(KEY_LAST_BONUS_DAY, -1)

        if (today != lastBonusDay) {
            resetFreeBonusRiddlesUsed()
            return 0
        }
        return prefs.getInt(KEY_FREE_BONUS_USED_COUNT, 0)
    }

    fun incrementFreeBonusRiddlesUsed() {
        val currentCount = getFreeBonusRiddlesUsedToday()
        if (currentCount < MAX_FREE_BONUS_RIDDLES) {
            prefs.edit { putInt(KEY_FREE_BONUS_USED_COUNT, currentCount + 1) }
        }
    }

    fun getFreeBonusRiddlesRemaining(): Int {
        return (MAX_FREE_BONUS_RIDDLES - getFreeBonusRiddlesUsedToday()).coerceAtLeast(0)
    }

    fun resetFreeBonusRiddlesUsed() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        prefs.edit {
            putInt(KEY_FREE_BONUS_USED_COUNT, 0)
            putInt(KEY_LAST_BONUS_DAY, today)
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false) // Default to false
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled) }
    }
}