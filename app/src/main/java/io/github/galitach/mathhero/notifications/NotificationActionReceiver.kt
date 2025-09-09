package io.github.galitach.mathhero.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.MathHeroApplication
import io.github.galitach.mathhero.data.MathProblemRepository
import io.github.galitach.mathhero.data.SharedPreferencesManager

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(NotificationReceiver.NOTIFICATION_ID)

        if (intent.action == ACTION_REVEAL) {
            val repository = MathProblemRepository(context, SharedPreferencesManager)
            val problem = repository.getCurrentProblem()
            val answer = problem.answer

            val heroPerson = Person.Builder()
                .setName(context.getString(R.string.app_name))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_math_hero_logo))
                .build()

            val answerMessage = NotificationCompat.MessagingStyle.Message(
                answer,
                System.currentTimeMillis(),
                heroPerson
            )

            val messagingStyle = NotificationCompat.MessagingStyle(heroPerson)
                .setConversationTitle(context.getString(R.string.notification_answer_title))
                .addMessage(answerMessage)

            val answerNotification = NotificationCompat.Builder(context, MathHeroApplication.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setStyle(messagingStyle)
                .setAutoCancel(true)
                .build()

            try {
                notificationManager.notify(ANSWER_NOTIFICATION_ID, answerNotification)
            } catch (_: SecurityException) {
                // Fails silently if permissions are revoked.
            }
        }
    }

    companion object {
        const val ACTION_REVEAL = "io.github.galitach.mathhero.ACTION_REVEAL"
        const val ACTION_DISMISS = "io.github.galitach.mathhero.ACTION_DISMISS"
        const val ANSWER_NOTIFICATION_ID = 2
    }
}