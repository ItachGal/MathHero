package io.github.galitach.mathhero.ui.hint

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.MathProblem
import io.github.galitach.mathhero.databinding.BottomSheetHintBinding

class HintBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetHintBinding? = null
    private val binding get() = _binding!!

    private val hintColors by lazy {
        listOf(
            ContextCompat.getColor(requireContext(), R.color.hint_color_1),
            ContextCompat.getColor(requireContext(), R.color.hint_color_2),
            ContextCompat.getColor(requireContext(), R.color.hint_color_3),
            ContextCompat.getColor(requireContext(), R.color.hint_color_4),
            ContextCompat.getColor(requireContext(), R.color.hint_color_5),
            ContextCompat.getColor(requireContext(), R.color.hint_color_6),
            ContextCompat.getColor(requireContext(), R.color.hint_color_7),
            ContextCompat.getColor(requireContext(), R.color.hint_color_8),
            ContextCompat.getColor(requireContext(), R.color.hint_color_9),
            ContextCompat.getColor(requireContext(), R.color.hint_color_10)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetHintBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val problem = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_PROBLEM, MathProblem::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_PROBLEM)
        }
        renderVisualHint(problem)
    }

    private fun renderVisualHint(problem: MathProblem?) {
        problem ?: return
        binding.hintGrid.removeAllViews()

        val answer = problem.answer.toIntOrNull() ?: 0

        val totalStars = when (problem.operator) {
            "+" -> (problem.num1 + problem.num2)
            "-" -> problem.num1
            "×" -> (problem.num1 * problem.num2)
            "÷" -> problem.num1
            else -> 0
        }.coerceAtMost(100)

        val starSize = when {
            totalStars > 90 -> resources.getDimensionPixelSize(R.dimen.hint_star_size_small)
            totalStars > 70 -> resources.getDimensionPixelSize(R.dimen.hint_star_size_medium)
            else -> resources.getDimensionPixelSize(R.dimen.hint_star_size_large)
        }

        val colorAddPrimary = ContextCompat.getColor(requireContext(), R.color.hint_add_primary)
        val colorAddSecondary = ContextCompat.getColor(requireContext(), R.color.hint_add_secondary)
        val colorMuted = ContextCompat.getColor(requireContext(), R.color.colorSurfaceVariant)

        when (problem.operator) {
            "+" -> {
                binding.hintGrid.columnCount = 10
                repeat(problem.num1) { addStarToHint(colorAddPrimary, starSize) }
                repeat(problem.num2) { addStarToHint(colorAddSecondary, starSize) }
            }
            "-" -> {
                binding.hintGrid.columnCount = 10
                repeat(problem.num1) { index ->
                    val color = if (index < answer) colorAddPrimary else colorMuted
                    addStarToHint(color, starSize)
                }
            }
            "×" -> {
                binding.hintGrid.columnCount = problem.num1.coerceAtLeast(1)
                repeat(totalStars) {
                    val rowIndex = it / problem.num1
                    val color = hintColors[rowIndex % hintColors.size]
                    addStarToHint(color, starSize)
                }
            }
            "÷" -> {
                binding.hintGrid.columnCount = answer.coerceAtLeast(1)
                repeat(totalStars) {
                    val rowIndex = it / answer
                    val color = hintColors[rowIndex % hintColors.size]
                    addStarToHint(color, starSize)
                }
            }
        }
    }

    private fun addStarToHint(color: Int, starSize: Int) {
        val star = ImageView(context).apply {
            layoutParams =
                android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = starSize
                    columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                    setMargins(2, 2, 2, 2)
                }
            setImageResource(R.drawable.ic_star_filled)
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
        binding.hintGrid.addView(star)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "HintBottomSheetFragment"
        private const val ARG_PROBLEM = "arg_problem"

        fun newInstance(problem: MathProblem): HintBottomSheetFragment {
            return HintBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PROBLEM, problem)
                }
            }
        }
    }
}