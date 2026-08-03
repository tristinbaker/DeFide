package com.tristinbaker.defide

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DeFideApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val novenaChannel = NotificationChannel(
            CHANNEL_NOVENA_REMINDERS,
            "Novena Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reminders for active novenas"
        }
        val rosaryChannel = NotificationChannel(
            CHANNEL_ROSARY_REMINDERS,
            "Rosary Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reminder to pray the rosary"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(novenaChannel)
        manager.createNotificationChannel(rosaryChannel)
    }

    companion object {
        const val CHANNEL_NOVENA_REMINDERS = "novena_reminders"
        const val CHANNEL_ROSARY_REMINDERS = "rosary_reminders"
    }
}
