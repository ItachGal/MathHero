package io.github.galitach.mathhero.data

import android.content.Context
import io.github.galitach.mathhero.R
import kotlin.math.max

object RecommendationEngine {

    private const val MIN_ATTEMPTS_FOR_INSIGHT = 10
    private const val SIGNIFICANT_DROP_THRESHOLD = 0.25 // 25% drop in accuracy

    fun generate(results: List<ProblemResult>, context: Context): List<Recommendation> {
        if (results.size < MIN_ATTEMPTS_FOR_INSIGHT) {
            return listOf(
                Recommendation(
                    R.drawable.ic_insight,
                    R.string.recommendation_more_data_title,
                    context.getString(R.string.recommendation_more_data_desc)
                )
            )
        }

        val recommendations = mutableListOf<Recommendation>()

        recommendations.addAll(findWeakestOperation(results, context))
        recommendations.addAll(findNumberRangeStruggles(results, context))

        if (recommendations.isEmpty()) {
            recommendations.add(
                Recommendation(
                    R.drawable.ic_check_circle,
                    R.string.recommendation_all_good_title,
                    context.getString(R.string.recommendation_all_good_desc)
                )
            )
        }

        return recommendations
    }

    private fun findWeakestOperation(results: List<ProblemResult>, context: Context): List<Recommendation> {
        val accuracyByOp = Operation.entries.map { op ->
            val opResults = results.filter { it.operation == op }
            if (opResults.size >= MIN_ATTEMPTS_FOR_INSIGHT) {
                val correct = opResults.count { it.wasCorrect }
                val accuracy = correct.toDouble() / opResults.size
                op to accuracy
            } else {
                null
            }
        }.filterNotNull().sortedBy { it.second }

        val weakest = accuracyByOp.firstOrNull()
        if (weakest != null && weakest.second < 0.7) { // If accuracy is below 70%
            val opName = context.getString(weakest.first.stringRes)
            return listOf(
                Recommendation(
                    R.drawable.ic_insight,
                    R.string.recommendation_weakest_op_title,
                    context.getString(R.string.recommendation_weakest_op_desc, opName)
                )
            )
        }
        return emptyList()
    }

    private fun findNumberRangeStruggles(results: List<ProblemResult>, context: Context): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()
        val ranges = listOf(0..20, 21..100, 101..Int.MAX_VALUE)

        Operation.entries.forEach { op ->
            val resultsByRange = ranges.map { range ->
                val rangeResults = results.filter {
                    val problemMaxNum = max(it.num1, it.num2)
                    it.operation == op && problemMaxNum in range
                }
                if (rangeResults.size >= MIN_ATTEMPTS_FOR_INSIGHT / 2) { // Lower threshold for ranges
                    val correct = rangeResults.count { it.wasCorrect }
                    range to (correct.toDouble() / rangeResults.size)
                } else {
                    null
                }
            }.filterNotNull()

            for (i in 0 until resultsByRange.size - 1) {
                val (range1, accuracy1) = resultsByRange[i]
                val (range2, accuracy2) = resultsByRange[i + 1]

                if (accuracy1 - accuracy2 > SIGNIFICANT_DROP_THRESHOLD) {
                    val opName = context.getString(op.stringRes)
                    recommendations.add(
                        Recommendation(
                            R.drawable.ic_insight,
                            R.string.recommendation_range_struggle_title,
                            context.getString(R.string.recommendation_range_struggle_desc, opName, range1.last, range2.first)
                        )
                    )
                    // Only add one range recommendation per operation to avoid spam
                    break
                }
            }
        }
        return recommendations
    }
}