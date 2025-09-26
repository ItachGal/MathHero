package io.github.galitach.mathhero.data

import android.content.Context
import java.util.Calendar
import kotlin.random.Random

class MathProblemRepository(
    private val context: Context,
    private val prefsManager: SharedPreferencesManager
) {

    fun getCurrentProblem(): MathProblem {
        val settings = prefsManager.getDifficultySettings()
        val streak = prefsManager.getStreakCount()
        // Use day of year and year as a seed for a deterministic daily problem
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        val seed = (year * 1000L) + dayOfYear
        return MathProblemGenerator.generateProblem(context, settings, streak, seed)
    }

    fun getBonusProblem(): MathProblem {
        val settings = prefsManager.getDifficultySettings()
        val streak = prefsManager.getStreakCount()
        // Use a random seed for a new, non-deterministic problem
        return MathProblemGenerator.generateProblem(context, settings, streak, Random.nextLong())
    }

    fun getKidModeProblem(settings: DifficultySettings, sessionStreak: Int): MathProblem {
        // Use a random seed for a new, non-deterministic problem
        // Use session streak for progressive difficulty within the session
        return MathProblemGenerator.generateProblem(context, settings, sessionStreak, Random.nextLong())
    }

    fun getArchivedProblems(): List<MathProblem> {
        return prefsManager.getArchivedProblems()
    }
}