package io.github.galitach.mathhero.ui.difficulty

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.DifficultyLevel
import io.github.galitach.mathhero.data.DifficultySettings
import io.github.galitach.mathhero.data.Operation
import io.github.galitach.mathhero.data.SharedPreferencesManager
import io.github.galitach.mathhero.databinding.DialogDifficultySelectionBinding
import io.github.galitach.mathhero.ui.MainViewModel
import io.github.galitach.mathhero.ui.MainViewModelFactory

class DifficultySelectionDialogFragment : DialogFragment() {

    private var _binding: DialogDifficultySelectionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels { MainViewModelFactory }
    private var isInteracting = false // To prevent listener feedback loops

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

        setupViews()
        loadCurrentSettings()
        setupListeners()
    }

    private fun setupViews() {
        DifficultyLevel.entries.forEach { level ->
            val radioButton = (layoutInflater.inflate(R.layout.item_difficulty_preset, binding.presetsGroup, false) as RadioButton).apply {
                id = View.generateViewId()
                text = getString(level.titleRes)
                tag = level
                setCompoundDrawablesWithIntrinsicBounds(level.iconRes, 0, 0, 0)
                TextViewCompat.setCompoundDrawableTintList(this, ContextCompat.getColorStateList(requireContext(), R.color.selector_preset_icon_tint))
                compoundDrawableTintMode = PorterDuff.Mode.SRC_IN
            }
            binding.presetsGroup.addView(radioButton)
        }

        Operation.entries.forEach { op ->
            val chip = (layoutInflater.inflate(R.layout.chip_operation, binding.operationsGroup, false) as Chip).apply {
                id = View.generateViewId()
                text = op.name.lowercase().replaceFirstChar { it.titlecase() }
                tag = op
            }
            binding.operationsGroup.addView(chip)
        }
    }

    private fun loadCurrentSettings() {
        isInteracting = false // Disable listeners during initial population
        val settings = SharedPreferencesManager.getDifficultySettings()
        syncUiWithSettings(settings)
        isInteracting = true // Re-enable listeners
    }

    private fun setupListeners() {
        binding.presetsGroup.setOnCheckedChangeListener { group, checkedId ->
            // Only react to user interaction and actual selections (not clearCheck)
            if (!isInteracting || checkedId == View.NO_ID) return@setOnCheckedChangeListener

            val selectedButton = group.findViewById<RadioButton>(checkedId)
            val selectedPreset = selectedButton?.tag as? DifficultyLevel

            selectedPreset?.let {
                // A preset was clicked. Temporarily disable listeners to prevent feedback loops,
                // update the custom controls to match the preset, then re-enable listeners.
                isInteracting = false
                updateCustomControls(it.settings)
                isInteracting = true
            }
        }

        // When a custom control is changed by the user, clear any selected preset.
        binding.operationsGroup.children.filterIsInstance<Chip>().forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                if (isInteracting) {
                    binding.presetsGroup.clearCheck()
                }
            }
        }

        binding.maxNumberSlider.addOnChangeListener { _, _, fromUser ->
            if (fromUser) {
                binding.presetsGroup.clearCheck()
            }
        }

        binding.maxNumberSlider.setLabelFormatter { value -> value.toInt().toString() }

        binding.saveButton.setOnClickListener {
            val selectedOps = binding.operationsGroup.checkedChipIds.mapNotNull { id ->
                binding.operationsGroup.findViewById<Chip>(id)?.tag as? Operation
            }.toSet()

            if (selectedOps.isEmpty()) {
                Toast.makeText(requireContext(), R.string.error_no_operation_selected, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maxNumber = binding.maxNumberSlider.value.toInt()
            viewModel.onDifficultySelected(DifficultySettings(selectedOps, maxNumber))
            dismiss()
        }
    }

    /**
     * Updates only the custom controls (chips and slider) based on the given settings.
     * This is called when a preset is selected.
     */
    private fun updateCustomControls(settings: DifficultySettings) {
        binding.operationsGroup.children.filterIsInstance<Chip>().forEach { chip ->
            val operation = chip.tag as Operation
            chip.isChecked = settings.operations.contains(operation)
        }
        binding.maxNumberSlider.value = settings.maxNumber.toFloat()
    }

    /**
     * Syncs the entire UI (presets and custom controls) with the given settings.
     * This is used for the initial setup.
     */
    private fun syncUiWithSettings(settings: DifficultySettings) {
        updateCustomControls(settings)

        val matchingPreset = DifficultyLevel.entries.find { it.settings == settings }
        if (matchingPreset != null) {
            binding.presetsGroup.children.filterIsInstance<RadioButton>()
                .find { it.tag == matchingPreset }
                ?.isChecked = true
        } else {
            binding.presetsGroup.clearCheck()
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