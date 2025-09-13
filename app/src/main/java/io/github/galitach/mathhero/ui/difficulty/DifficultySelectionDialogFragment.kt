package io.github.galitach.mathhero.ui.difficulty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.databinding.DialogDifficultySelectionBinding
import io.github.galitach.mathhero.ui.MainViewModel
import io.github.galitach.mathhero.ui.MainViewModelFactory

class DifficultySelectionDialogFragment : DialogFragment() {

    private var _binding: DialogDifficultySelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels { MainViewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogDifficultySelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isFirstTime = arguments?.getBoolean(ARG_IS_FIRST_TIME) ?: false
        if (isFirstTime) {
            binding.toolbar.navigationIcon = null
            isCancelable = false
        } else {
            binding.toolbar.setNavigationOnClickListener { dismiss() }
        }

        binding.difficultyRecyclerView.adapter = DifficultyAdapter { level ->
            viewModel.onDifficultySelected(level.settings)
            dismiss()
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
        const val TAG = "DifficultySelectionDialogFragment"
        private const val ARG_IS_FIRST_TIME = "arg_is_first_time"

        fun newInstance(isFirstTime: Boolean = false): DifficultySelectionDialogFragment {
            return DifficultySelectionDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_FIRST_TIME, isFirstTime)
                }
            }
        }
    }
}