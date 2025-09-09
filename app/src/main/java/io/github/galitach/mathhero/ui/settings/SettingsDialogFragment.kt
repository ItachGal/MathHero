package io.github.galitach.mathhero.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.snackbar.Snackbar
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.data.SharedPreferencesManager
import io.github.galitach.mathhero.databinding.DialogSettingsBinding
import io.github.galitach.mathhero.notifications.NotificationScheduler

class SettingsDialogFragment : DialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableNotifications(true)
            } else {
                binding.notificationSwitch.isChecked = false
                showPermissionSnackbar()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        setupSwitch()
    }

    private fun setupSwitch() {
        binding.notificationSwitch.isChecked = SharedPreferencesManager.areNotificationsEnabled()
        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestNotificationPermission()
            } else {
                enableNotifications(false)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    enableNotifications(true)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionSnackbar()
                    binding.notificationSwitch.isChecked = false
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            enableNotifications(true)
        }
    }

    private fun enableNotifications(enabled: Boolean) {
        SharedPreferencesManager.setNotificationsEnabled(enabled)
        if (enabled) {
            NotificationScheduler.scheduleDailyNotification(requireContext())
        } else {
            NotificationScheduler.cancelDailyNotification(requireContext())
        }
    }

    private fun showPermissionSnackbar() {
        Snackbar.make(binding.root, R.string.notification_permission_required, Snackbar.LENGTH_LONG)
            .setAction(R.string.action_settings) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
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
        const val TAG = "SettingsDialogFragment"
    }
}