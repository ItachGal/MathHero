package io.github.galitach.mathhero.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import io.github.galitach.mathhero.R
import io.github.galitach.mathhero.MathHeroApplication
import io.github.galitach.mathhero.data.MathProblemRepository
import io.github.galitach.mathhero.data.SharedPreferencesManager
import io.github.galitach.mathhero.ui.MainActivity

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = MathProblemRepository(context, SharedPreferencesManager)
        val problem = repository.getCurrentProblem()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            `package` = context.packageName
        }
        val openAppPendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val revealIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REVEAL
        }
        val revealPendingIntent = PendingIntent.getBroadcast(
            context, 1, revealIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, 2, dismissIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationText = problem.question

        val heroPerson = Person.Builder()
            .setName(context.getString(R.string.app_name))
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_math_hero_logo))
            .build()

        val message = NotificationCompat.MessagingStyle.Message(
            notificationText,
            System.currentTimeMillis(),
            heroPerson
        )

        val messagingStyle = NotificationCompat.MessagingStyle(heroPerson)
            .addMessage(message)

        val builder = NotificationCompat.Builder(context, MathHeroApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setStyle(messagingStyle)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.notification_action_reveal),
                    revealPendingIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    null,
                    context.getString(R.string.notification_action_solved),
                    dismissPendingIntent
                ).build()
            )

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (_: SecurityException) {
                // Permission might have been revoked.
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
    }
}