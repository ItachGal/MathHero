package io.github.galitach.mathhero.ui.progress

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.Recommendation
import io.github.galitach.mathhero.databinding.DialogRecommendationDetailBinding

class RecommendationDetailDialogFragment : DialogFragment() {

    private var _binding: DialogRecommendationDetailBinding? = null
    private val binding get() = _binding!!

    private val recommendation: Recommendation? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(ARG_RECOMMENDATION, Recommendation::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(ARG_RECOMMENDATION)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRecommendationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }

        recommendation?.let {
            binding.toolbar.setTitle(it.detailTitleRes)
            binding.recommendationDetailDescription.text = it.detailDescription
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
        const val TAG = "RecommendationDetailDialogFragment"
        private const val ARG_RECOMMENDATION = "arg_recommendation"

        fun newInstance(recommendation: Recommendation): RecommendationDetailDialogFragment {
            return RecommendationDetailDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_RECOMMENDATION, recommendation)
                }
            }
        }
    }
}