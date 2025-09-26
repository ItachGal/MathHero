package io.github.galitach.mathhero.ui.kidmode

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.Operation
import io.github.galitach.mathhero.data.ProblemResult
import io.github.galitach.mathhero.databinding.DialogKidModeSummaryBinding
import io.github.galitach.mathhero.databinding.ItemInsightBinding

class KidModeSummaryDialogFragment : DialogFragment() {

    interface OnKidModeSummaryDismissedListener {
        fun onKidModeSummaryDismissed()
    }

    private var _binding: DialogKidModeSummaryBinding? = null
    private val binding get() = _binding!!
    private var listener: OnKidModeSummaryDismissedListener? = null

    private val sessionResults: List<ProblemResult> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelableArrayList(ARG_RESULTS, ProblemResult::class.java) ?: emptyList()
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelableArrayList(ARG_RESULTS) ?: emptyList()
        }
    }

    private val rankName: String by lazy {
        arguments?.getString(ARG_RANK_NAME) ?: ""
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = parentFragment as? OnKidModeSummaryDismissedListener
        if (listener == null) {
            listener = context as? OnKidModeSummaryDismissedListener
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogKidModeSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        binding.toolbar.setNavigationIcon(null) // No back button
        binding.doneButton.setOnClickListener { dismiss() }

        displaySummary()
    }

    private fun displaySummary() {
        if (sessionResults.isEmpty()) {
            binding.summaryCongratsText.text = getString(R.string.kid_mode_summary_oops)
            binding.summaryScoreText.text = getString(R.string.kid_mode_summary_no_results)
            binding.insightsCard.isVisible = false
            return
        }

        binding.summaryCongratsText.text = getString(R.string.kid_mode_summary_congrats, rankName)

        val totalCorrect = sessionResults.count { it.wasCorrect }
        val totalAttempted = sessionResults.size
        binding.summaryScoreText.text = getString(R.string.kid_mode_summary_score, totalCorrect, totalAttempted)

        generateInsights()
    }

    private fun generateInsights() {
        val totalCorrect = sessionResults.count { it.wasCorrect }
        val totalAttempted = sessionResults.size

        binding.summaryInsightsList.removeAllViews()

        // Insight 1: Perfect score - The ultimate praise
        if (totalCorrect == totalAttempted && totalAttempted > 0) {
            addInsight(getString(R.string.kid_mode_insight_perfect), R.drawable.ic_star_filled)
            return // No need for other insights if it was perfect
        }

        // Calculate stats for each operation
        val statsByOp = mutableMapOf<Operation, Pair<Int, Int>>() // Correct, Total
        for (result in sessionResults) {
            val current = statsByOp.getOrDefault(result.operation, Pair(0, 0))
            val newCorrect = current.first + if (result.wasCorrect) 1 else 0
            val newTotal = current.second + 1
            statsByOp[result.operation] = Pair(newCorrect, newTotal)
        }

        val accuracyByOp = statsByOp.mapNotNull { (op, stats) ->
            if (stats.second > 0) {
                op to (stats.first.toDouble() / stats.second)
            } else {
                null
            }
        }.sortedByDescending { it.second }

        // Insight 2: Best operation (Strength)
        val bestOp = accuracyByOp.firstOrNull()
        if (bestOp != null && bestOp.second >= 0.8 && statsByOp[bestOp.first]!!.second >= 3) { // At least 80% and 3 attempts
            val opName = getString(bestOp.first.stringRes)
            addInsight(getString(R.string.kid_mode_insight_best_op, opName), getIconForOperation(bestOp.first))
        }

        // Insight 3: Weakest operation (Supportive suggestion)
        val weakestOp = accuracyByOp.lastOrNull()
        if (weakestOp != null && weakestOp.second < 0.6 && statsByOp[weakestOp.first]!!.second >= 4) { // Less than 60% and at least 4 attempts
            val opName = getString(weakestOp.first.stringRes)
            addInsight(getString(R.string.kid_mode_insight_weakest_op, opName), R.drawable.ic_insight)
        }

        // Insight 4: General encouragement if no specific insights were added
        if (binding.summaryInsightsList.childCount == 0) {
            if (totalAttempted > 0 && totalCorrect.toDouble() / totalAttempted >= 0.7) {
                addInsight(getString(R.string.kid_mode_insight_great_effort), R.drawable.ic_check_circle)
            }
        }

        binding.insightsCard.isVisible = binding.summaryInsightsList.childCount > 0
    }


    private fun addInsight(text: String, iconRes: Int) {
        val insightBinding = ItemInsightBinding.inflate(layoutInflater, binding.summaryInsightsList, false)
        insightBinding.insightIcon.setImageResource(iconRes)
        insightBinding.insightText.text = text
        binding.summaryInsightsList.addView(insightBinding.root)
    }

    private fun getIconForOperation(operation: Operation): Int {
        return when(operation) {
            Operation.ADDITION -> R.drawable.ic_op_plus_small
            Operation.SUBTRACTION -> R.drawable.ic_op_minus_small
            Operation.MULTIPLICATION -> R.drawable.ic_op_multiply_small
            Operation.DIVISION -> R.drawable.ic_op_divide_small
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.onKidModeSummaryDismissed()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }



    override fun getTheme(): Int {
        return R.style.FullScreenDialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "KidModeSummaryDialogFragment"
        private const val ARG_RESULTS = "arg_results"
        private const val ARG_RANK_NAME = "arg_rank_name"

        fun newInstance(results: ArrayList<ProblemResult>, rankName: String): KidModeSummaryDialogFragment {
            return KidModeSummaryDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelableArrayList(ARG_RESULTS, results)
                    putString(ARG_RANK_NAME, rankName)
                }
            }
        }
    }
}