package com.tristinbaker.defide.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tristinbaker.defide.DeFideApplication
import com.tristinbaker.defide.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NovenaReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            applicationContext,
            DeFideApplication.CHANNEL_NOVENA_REMINDERS,
        )
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Novena Reminder")
            .setContentText("Time for today's novena prayer.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        applicationContext
            .getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)

        return Result.success()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "novena_daily_reminder"
    }
}
