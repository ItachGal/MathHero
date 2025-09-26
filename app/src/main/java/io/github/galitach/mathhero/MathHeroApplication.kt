package io.github.galitach.mathhero

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import io.github.galitach.mathhero.billing.BillingManager
import io.github.galitach.mathhero.data.ProgressRepository
import io.github.galitach.mathhero.data.SharedPreferencesManager
import io.github.galitach.mathhero.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MathHeroApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var billingManager: BillingManager
        private set

    private val database by lazy { AppDatabase.getDatabase(this) }
    val progressRepository by lazy { ProgressRepository(database.problemResultDao()) }


    override fun onCreate() {
        super.onCreate()
        SharedPreferencesManager.initialize(this)
        billingManager = BillingManager(this, applicationScope)
        createNotificationChannel()

        // One-time migration from SharedPreferences to Room
        applicationScope.launch(Dispatchers.IO) {
            SharedPreferencesManager.migrateProgressDataIfNeeded(progressRepository)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "DAILY_RIDDLE_CHANNEL"
    }
}