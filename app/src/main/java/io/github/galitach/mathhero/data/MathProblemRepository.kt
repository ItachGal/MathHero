package io.github.galitach.mathhero.data

import android.content.Context
import java.util.Calendar
import kotlin.random.Random

class MathProblemRepository(
    private val context: Context,
    private val prefsManager: SharedPreferencesManager
) {

    fun getCurrentProblem(): MathProblem {
        // Use day of year as a seed for a deterministic daily problem
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return MathProblemGenerator.generateProblem(context, dayOfYear.toLong())
    }

    fun getBonusProblem(): MathProblem {
        // Use a random seed for a new, non-deterministic problem
        return MathProblemGenerator.generateProblem(context, Random.nextLong())
    }

    fun getArchivedProblems(): List<MathProblem> {
        return prefsManager.getArchivedProblems()
    }
}