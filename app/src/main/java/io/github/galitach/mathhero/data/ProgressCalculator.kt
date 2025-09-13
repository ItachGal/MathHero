package io.github.galitach.mathhero.data

import java.util.Calendar
import java.util.concurrent.TimeUnit

object ProgressCalculator {

    fun generateReport(results: List<ProblemResult>, highestStreak: Int): ProgressReport {
        val accuracyMap = mutableMapOf<Operation, Pair<Int, Int>>()
        Operation.entries.forEach { op ->
            val relevantResults = results.filter { it.operation == op }
            val correct = relevantResults.count { it.wasCorrect }
            val total = relevantResults.size
            if (total > 0) {
                accuracyMap[op] = Pair(correct, total)
            }
        }

        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val solvedLast7Days = results.count { it.timestamp >= sevenDaysAgo }

        val totalSolved = results.size

        val averagePerDay = if (results.isNotEmpty()) {
            val firstDay = Calendar.getInstance().apply { timeInMillis = results.last().timestamp }
            val today = Calendar.getInstance()
            val days = TimeUnit.MILLISECONDS.toDays(today.timeInMillis - firstDay.timeInMillis).coerceAtLeast(1)
            totalSolved / days.toDouble()
        } else {
            0.0
        }

        return ProgressReport(
            accuracyByOperation = accuracyMap,
            problemsSolvedLast7Days = solvedLast7Days,
            totalProblemsSolved = totalSolved,
            longestStreak = highestStreak,
            averageProblemsPerDay = averagePerDay
        )
    }
}