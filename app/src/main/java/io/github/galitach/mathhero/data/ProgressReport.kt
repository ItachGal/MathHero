package io.github.galitach.mathhero.data

data class ProgressReport(
    val accuracyByOperation: Map<Operation, Pair<Int, Int>>, // Correct, Total
    val problemsSolvedLast7Days: Int,
    val totalProblemsSolved: Int,
    val longestStreak: Int,
    val averageProblemsPerDay: Double,
    val recommendations: List<Recommendation>
)