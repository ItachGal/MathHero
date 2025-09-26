package io.github.galitach.mathhero.data

import android.content.Context
import io.github.galitach.mathhero.R
import kotlin.math.max

object RecommendationEngine {

    private const val MIN_ATTEMPTS_FOR_INSIGHT = 10
    private const val SIGNIFICANT_DROP_THRESHOLD = 0.25 // 25% drop in accuracy

    fun generate(results: List<ProblemResult>, context: Context): List<Recommendation> {
        val dismissedMap = SharedPreferencesManager.getDismissedRecommendations()

        if (results.size < MIN_ATTEMPTS_FOR_INSIGHT) {
            return if ("more_data" in dismissedMap) emptyList() else listOf(
                Recommendation(
                    id = "more_data",
                    iconRes = R.drawable.ic_insight,
                    titleRes = R.string.recommendation_more_data_title,
                    description = context.getString(R.string.recommendation_more_data_desc),
                    detailTitle = context.getString(R.string.recommendation_more_data_title),
                    detailDescription = context.getString(R.string.recommendation_more_data_detail_desc)
                )
            )
        }

        val recommendations = mutableListOf<Recommendation>()
        recommendations.addAll(findWeakestOperation(results, context, dismissedMap))
        recommendations.addAll(findNumberRangeStruggles(results, context, dismissedMap))

        if (recommendations.isEmpty() && "all_good" !in dismissedMap) {
            return listOf(
                Recommendation(
                    id = "all_good",
                    iconRes = R.drawable.ic_check_circle,
                    titleRes = R.string.recommendation_all_good_title,
                    description = context.getString(R.string.recommendation_all_good_desc),
                    detailTitle = context.getString(R.string.recommendation_all_good_title),
                    detailDescription = context.getString(R.string.recommendation_all_good_detail_desc)
                )
            )
        }

        return recommendations
    }

    private fun findWeakestOperation(
        results: List<ProblemResult>,
        context: Context,
        dismissedMap: Map<String, Long>
    ): List<Recommendation> {
        val lastDismissalTime = dismissedMap.filter { it.key.startsWith("weakest_op_") }
            .maxOfOrNull { it.value } ?: 0L

        val recentResults = results.filter { it.timestamp > lastDismissalTime }

        if (recentResults.size < MIN_ATTEMPTS_FOR_INSIGHT) return emptyList()

        val accuracyByOp = Operation.entries.mapNotNull { op ->
            val opResults = recentResults.filter { it.operation == op }
            if (opResults.size >= MIN_ATTEMPTS_FOR_INSIGHT) {
                val correct = opResults.count { it.wasCorrect }
                val accuracy = correct.toDouble() / opResults.size
                op to accuracy
            } else {
                null
            }
        }.sortedBy { it.second }

        val weakest = accuracyByOp.firstOrNull()
        if (weakest != null && weakest.second < 0.7) { // If accuracy is below 70%
            val opName = context.getString(weakest.first.stringRes)
            return listOf(
                Recommendation(
                    id = "weakest_op_${weakest.first.name}",
                    iconRes = R.drawable.ic_insight,
                    titleRes = R.string.recommendation_weakest_op_title,
                    description = context.getString(R.string.recommendation_weakest_op_desc, opName),
                    detailTitle = context.getString(R.string.recommendation_weakest_op_detail_title, opName),
                    detailDescription = getDetailForWeakestOperation(weakest.first, context)
                )
            )
        }
        return emptyList()
    }

    private fun getDetailForWeakestOperation(op: Operation, context: Context): String {
        val opName = context.getString(op.stringRes)
        val baseString = context.getString(R.string.recommendation_weakest_op_detail_desc, opName)
        val detailStringRes = when (op) {
            Operation.ADDITION -> R.string.recommendation_detail_weakest_addition
            Operation.SUBTRACTION -> R.string.recommendation_detail_weakest_subtraction
            Operation.MULTIPLICATION -> R.string.recommendation_detail_weakest_multiplication
            Operation.DIVISION -> R.string.recommendation_detail_weakest_division
        }
        return baseString + "\n\n" + context.getString(detailStringRes)
    }

    private fun findNumberRangeStruggles(
        results: List<ProblemResult>,
        context: Context,
        dismissedMap: Map<String, Long>
    ): List<Recommendation> {
        val lastDismissalTime = dismissedMap.filter { it.key.startsWith("range_struggle_") }
            .maxOfOrNull { it.value } ?: 0L

        val recentResults = results.filter { it.timestamp > lastDismissalTime }
        if (recentResults.size < MIN_ATTEMPTS_FOR_INSIGHT) return emptyList()

        val recommendations = mutableListOf<Recommendation>()
        val ranges = listOf(0..20, 21..100, 101..Int.MAX_VALUE)

        Operation.entries.forEach { op ->
            val resultsByRange = ranges.mapNotNull { range ->
                val rangeResults = recentResults.filter {
                    val problemMaxNum = max(it.num1, it.num2)
                    it.operation == op && problemMaxNum in range
                }
                if (rangeResults.size >= MIN_ATTEMPTS_FOR_INSIGHT / 2) { // Lower threshold for ranges
                    val correct = rangeResults.count { it.wasCorrect }
                    range to (correct.toDouble() / rangeResults.size)
                } else {
                    null
                }
            }

            for (i in 0 until resultsByRange.size - 1) {
                val (range1, accuracy1) = resultsByRange[i]
                val (range2, accuracy2) = resultsByRange[i + 1]

                if (accuracy1 - accuracy2 > SIGNIFICANT_DROP_THRESHOLD) {
                    val opName = context.getString(op.stringRes)
                    recommendations.add(
                        Recommendation(
                            id = "range_struggle_${op.name}_${range1.last}_${range2.first}",
                            iconRes = R.drawable.ic_insight,
                            titleRes = R.string.recommendation_range_struggle_title,
                            description = context.getString(R.string.recommendation_range_struggle_desc, opName, range1.last, range2.first),
                            detailTitle = context.getString(R.string.recommendation_range_struggle_detail_title),
                            detailDescription = getDetailForRangeStruggle(op, context)
                        )
                    )
                    break
                }
            }
        }
        return recommendations
    }

    private fun getDetailForRangeStruggle(op: Operation, context: Context): String {
        return when (op) {
            Operation.ADDITION -> context.getString(R.string.recommendation_detail_addition_large)
            Operation.SUBTRACTION -> context.getString(R.string.recommendation_detail_subtraction_large)
            Operation.MULTIPLICATION -> context.getString(R.string.recommendation_detail_multiplication_large)
            Operation.DIVISION -> context.getString(R.string.recommendation_detail_division_large)
        }
    }
}