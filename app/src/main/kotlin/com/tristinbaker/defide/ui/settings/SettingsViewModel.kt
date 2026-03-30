package com.tristinbaker.defide.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tristinbaker.defide.data.preferences.AppTheme
import com.tristinbaker.defide.data.preferences.UserPreferences
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import com.tristinbaker.defide.worker.NovenaReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = prefsRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefsRepository.setTheme(theme) }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch {
            prefsRepository.setAppLanguage(language)
            if (language == "pt") {
                val currentTrans = prefsRepository.preferences.first().bibleTranslationId
                if (currentTrans != "ave-maria") {
                    prefsRepository.setBibleTranslation("ave-maria")
                }
            }
        }
    }

    fun setBibleTranslation(translationId: String) {
        viewModelScope.launch { prefsRepository.setBibleTranslation(translationId) }
    }

    fun setNovenaNotificationTime(time: String) {
        viewModelScope.launch {
            prefsRepository.setNovenaNotificationTime(time)
            if (time.isNotEmpty()) scheduleReminder(time) else cancelReminder()
        }
    }

    private fun scheduleReminder(time: String) {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        val initialDelay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<NovenaReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NovenaReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun cancelReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(NovenaReminderWorker.WORK_NAME)
    }

    fun setBibleStreakGoal(goal: Int) {
        viewModelScope.launch { prefsRepository.setBibleStreakGoal(goal) }
    }
}
