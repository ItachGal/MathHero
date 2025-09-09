package io.github.galitach.mathhero.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.galitach.mathhero.data.SharedPreferencesManager

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            SharedPreferencesManager.initialize(context)
            if (SharedPreferencesManager.areNotificationsEnabled()) {
                NotificationScheduler.scheduleDailyNotification(context)
            }
        }
    }
}