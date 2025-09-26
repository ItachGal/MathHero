package io.github.galitach.mathhero.ui.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.progressindicator.LinearProgressIndicator
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.Operation
import io.github.galitach.mathhero.data.ProgressCalculator
import io.github.galitach.mathhero.data.SharedPreferencesManager
import io.github.galitach.mathhero.databinding.DialogProgressBinding
import io.github.galitach.mathhero.databinding.ItemProgressAccuracyBinding
import io.github.galitach.mathhero.databinding.ItemRecommendationBinding
import io.github.galitach.mathhero.ui.MainViewModel
import io.github.galitach.mathhero.ui.MainViewModelFactory
import io.github.galitach.mathhero.ui.upgrade.UpgradeDialogFragment
import java.text.DecimalFormat
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProgressDialogFragment : DialogFragment() {

    private var _binding: DialogProgressBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { MainViewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogProgressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.upgradeButton.setOnClickListener {
            UpgradeDialogFragment.newInstance().show(parentFragmentManager, UpgradeDialogFragment.TAG)
        }
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.map { it.isPro }.distinctUntilChanged().collect { isPro ->
                    updateUi(isPro)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.map { it.proProductDetails }.distinctUntilChanged().collect { details ->
                    details?.oneTimePurchaseOfferDetails?.let {
                        binding.upgradeButton.text = getString(R.string.upgrade_to_pro_price, it.formattedPrice)
                    }
                }
            }
        }
    }

    private fun updateUi(isPro: Boolean) {
        binding.lockedView.isVisible = !isPro
        binding.unlockedView.isVisible = isPro

        if (isPro) {
            loadProgressData()
        }
    }

    private fun loadProgressData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val results = viewModel.getProgressResults()
            val highestStreak = SharedPreferencesManager.getHighestStreakCount()
            val report = ProgressCalculator.generateReport(requireContext(), results, highestStreak)

            binding.totalSolvedText.text = getString(R.string.progress_total_solved, report.totalProblemsSolved)
            binding.last7DaysText.text = getString(R.string.progress_last_7_days, report.problemsSolvedLast7Days)
            val df = DecimalFormat("#.##")
            binding.averagePerDayText.text = getString(R.string.progress_average_per_day, df.format(report.averageProblemsPerDay))
            binding.longestStreakText.text = getString(R.string.progress_longest_streak, report.longestStreak)

            // Populate Accuracy
            binding.accuracyContainer.removeAllViews()
            if (report.accuracyByOperation.isEmpty()) {
                val emptyView = TextView(requireContext()).apply {
                    text = getString(R.string.progress_no_accuracy_data)
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                }
                binding.accuracyContainer.addView(emptyView)
            } else {
                report.accuracyByOperation.entries.sortedBy { it.key.ordinal }.forEach { (op, pair) ->
                    val (correct, total) = pair
                    val accuracyBinding = ItemProgressAccuracyBinding.inflate(layoutInflater, binding.accuracyContainer, false)
                    accuracyBinding.operationIcon.setImageResource(getIconForOperation(op))
                    accuracyBinding.operationName.text = op.name.lowercase().replaceFirstChar { it.titlecase() }
                    val percentage = if (total > 0) (correct.toFloat() / total * 100).toInt() else 0
                    accuracyBinding.accuracyPercentage.text = getString(R.string.progress_percentage, percentage)
                    accuracyBinding.accuracyProgress.progress = percentage
                    accuracyBinding.accuracyDetails.text = getString(R.string.progress_accuracy_details, correct, total)
                    binding.accuracyContainer.addView(accuracyBinding.root)
                }
            }

            // Populate Recommendations
            binding.recommendationsContainer.removeAllViews()
            if (report.recommendations.isNotEmpty()) {
                binding.recommendationsSection.isVisible = true
                report.recommendations.forEach { recommendation ->
                    val recommendationBinding = ItemRecommendationBinding.inflate(layoutInflater, binding.recommendationsContainer, false)
                    recommendationBinding.recommendationIcon.setImageResource(recommendation.iconRes)
                    recommendationBinding.recommendationTitle.setText(recommendation.titleRes)
                    recommendationBinding.recommendationDescription.text = recommendation.description
                    binding.recommendationsContainer.addView(recommendationBinding.root)
                }
            } else {
                binding.recommendationsSection.isVisible = false
            }
        }
    }

    private fun getIconForOperation(operation: Operation): Int {
        return when(operation) {
            Operation.ADDITION -> R.drawable.ic_op_plus_small
            Operation.SUBTRACTION -> R.drawable.ic_op_minus_small
            Operation.MULTIPLICATION -> R.drawable.ic_op_multiply_small
            Operation.DIVISION -> R.drawable.ic_op_divide_small
        }
    }

    override fun getTheme(): Int {
        return R.style.FullScreenDialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ProgressDialogFragment"
        fun newInstance(): ProgressDialogFragment {
            return ProgressDialogFragment()
        }
    }
}