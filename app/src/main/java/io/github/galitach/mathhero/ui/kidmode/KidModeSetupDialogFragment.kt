package io.github.galitach.mathhero.ui.kidmode

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.DifficultyLevel
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

        binding.startButton.setOnClickListener {
            checkPinningAndStart()
        }
    }

    private fun checkPinningAndStart() {
        val dpm = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        if (!dpm.isLockTaskPermitted(requireContext().packageName)) {
            showPinningNotEnabledDialog()
        } else {
            startSession()
        }
    }

    private fun startSession() {
        val targetChipId = binding.targetGroup.checkedChipId
        val difficultyChipId = binding.difficultyGroup.checkedChipId

        if (targetChipId == View.NO_ID || difficultyChipId == View.NO_ID) {
            Toast.makeText(requireContext(), R.string.kid_mode_error_selection, Toast.LENGTH_SHORT).show()
            return
        }

        val target = binding.root.findViewById<Chip>(targetChipId).text.toString().toInt()
        val difficulty = when (difficultyChipId) {
            R.id.difficulty_novice_chip -> DifficultyLevel.NOVICE
            R.id.difficulty_apprentice_chip -> DifficultyLevel.APPRENTICE
            R.id.difficulty_adept_chip -> DifficultyLevel.ADEPT
            else -> DifficultyLevel.NOVICE
        }

        viewModel.onKidModeSetup(target, difficulty.settings)
        dismiss()
    }

    private fun showPinningNotEnabledDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.kid_mode_pinning_disabled_title)
            .setMessage(R.string.kid_mode_pinning_disabled_desc)
            .setPositiveButton(R.string.action_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
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