package io.github.galitach.mathhero.ui.kidmode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.DifficultySettings
import io.github.galitach.mathhero.data.Operation
import io.github.galitach.mathhero.databinding.DialogKidModeSetupBinding
import io.github.galitach.mathhero.ui.MainViewModel
import io.github.galitach.mathhero.ui.MainViewModelFactory

class KidModeSetupDialogFragment : DialogFragment() {

    private var _binding: DialogKidModeSetupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels { MainViewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogKidModeSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }

        setupDifficultyControls()

        binding.startButton.setOnClickListener {
            startSession()
        }
    }

    private fun setupDifficultyControls() {
        // Populate operation chips
        Operation.entries.forEach { op ->
            val chip = (layoutInflater.inflate(R.layout.chip_operation, binding.operationsGroup, false) as Chip).apply {
                id = View.generateViewId()
                text = getString(op.stringRes)
                tag = op
            }
            binding.operationsGroup.addView(chip)
        }

        // Set default selection (Addition)
        (binding.operationsGroup.findViewWithTag(Operation.ADDITION) as? Chip)?.isChecked = true

        binding.maxNumberSlider.setLabelFormatter { value -> value.toInt().toString() }
    }

    private fun startSession() {
        val targetChipId = binding.targetGroup.checkedChipId

        if (targetChipId == View.NO_ID) {
            Toast.makeText(requireContext(), R.string.kid_mode_error_selection_target, Toast.LENGTH_SHORT).show()
            return
        }

        val selectedOps = binding.operationsGroup.checkedChipIds.mapNotNull { id ->
            binding.operationsGroup.findViewById<Chip>(id)?.tag as? Operation
        }.toSet()

        if (selectedOps.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_no_operation_selected, Toast.LENGTH_SHORT).show()
            return
        }

        val target = binding.root.findViewById<Chip>(targetChipId).text.toString().toInt()
        val maxNumber = binding.maxNumberSlider.value.toInt()
        val difficultySettings = DifficultySettings(selectedOps, maxNumber)

        viewModel.onKidModeSetup(target, difficultySettings)
        dismiss()
    }

    override fun getTheme(): Int {
        return R.style.FullScreenDialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "KidModeSetupDialogFragment"
        fun newInstance(): KidModeSetupDialogFragment {
            return KidModeSetupDialogFragment()
        }
    }
}