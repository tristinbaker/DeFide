package com.tristinbaker.defide.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tristinbaker.defide.data.backup.BackupManager
import com.tristinbaker.defide.data.preferences.AppFont
import com.tristinbaker.defide.data.preferences.AppRite
import com.tristinbaker.defide.data.preferences.AppTheme
import com.tristinbaker.defide.data.preferences.BackupFrequency
import com.tristinbaker.defide.data.preferences.RosaryOrder
import com.tristinbaker.defide.data.preferences.UserPreferences
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.tristinbaker.defide.widget.VotdWidget
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import com.tristinbaker.defide.worker.BackupWorker
import com.tristinbaker.defide.worker.NovenaReminderWorker
import com.tristinbaker.defide.worker.RosaryReminderWorker
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
    private val backupManager: BackupManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = prefsRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _backupMessage = MutableSharedFlow<String>()
    val backupMessage: SharedFlow<String> = _backupMessage.asSharedFlow()

    fun backup(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.exportTo(uri)
            _backupMessage.emit(
                if (result.isSuccess) context.getString(com.tristinbaker.defide.R.string.backup_success)
                else context.getString(com.tristinbaker.defide.R.string.backup_failed)
            )
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            val result = backupManager.importFrom(uri)
            _backupMessage.emit(
                if (result.isSuccess) context.getString(com.tristinbaker.defide.R.string.restore_success)
                else context.getString(com.tristinbaker.defide.R.string.restore_failed)
            )
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefsRepository.setTheme(theme) }
    }

    fun setAppFont(font: AppFont) {
        viewModelScope.launch { prefsRepository.setAppFont(font) }
    }

    fun setAppLanguage(language: String) {
        viewModelScope.launch {
            prefsRepository.setAppLanguage(language)
            val defaultTranslation = when (language) {
                "es"    -> "platense"
                "pt-BR" -> "ave-maria"
                "pt-PT" -> "porcap"
                "fr"    -> "crampon"
                "lt"    -> "rk1998"
                "zh-CN" -> "sg"
                "it"    -> "mar"
                else    -> "dra"
            }
            prefsRepository.setBibleTranslation(defaultTranslation)
            refreshVotdWidget()
        }
    }

    fun setBibleTranslation(translationId: String) {
        viewModelScope.launch {
            prefsRepository.setBibleTranslation(translationId)
            refreshVotdWidget()
        }
    }

    private suspend fun refreshVotdWidget() {
        GlanceAppWidgetManager(context)
            .getGlanceIds(VotdWidget::class.java)
            .forEach { VotdWidget().update(context, it) }
    }

    fun setNovenaNotificationTime(time: String) {
        viewModelScope.launch {
            prefsRepository.setNovenaNotificationTime(time)
            if (time.isNotEmpty()) scheduleDailyReminder<NovenaReminderWorker>(time, NovenaReminderWorker.WORK_NAME)
            else cancelReminder(NovenaReminderWorker.WORK_NAME)
        }
    }

    fun setRosaryNotificationTime(time: String) {
        viewModelScope.launch {
            prefsRepository.setRosaryNotificationTime(time)
            if (time.isNotEmpty()) scheduleDailyReminder<RosaryReminderWorker>(time, RosaryReminderWorker.WORK_NAME)
            else cancelReminder(RosaryReminderWorker.WORK_NAME)
        }
    }

    private inline fun <reified W : androidx.work.ListenableWorker> scheduleDailyReminder(time: String, workName: String) {
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

        val request = PeriodicWorkRequestBuilder<W>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun cancelReminder(workName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }

    fun setBibleStreakGoal(goal: Int) {
        viewModelScope.launch { prefsRepository.setBibleStreakGoal(goal) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setKeepScreenOn(enabled) }
    }

    fun setFullScreenMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setFullScreenMode(enabled) }
    }

    fun setRosaryOrder(order: RosaryOrder) {
        viewModelScope.launch { prefsRepository.setRosaryOrder(order) }
    }

    fun setRosaryHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setRosaryHapticFeedback(enabled) }
    }

    fun setRosaryNarrationEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setRosaryNarrationEnabled(enabled) }
    }

    fun setRosaryIntentionInDesign(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setRosaryIntentionInDesign(enabled) }
    }

    fun setAutoBackupFolder(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        viewModelScope.launch {
            prefsRepository.setAutoBackupFolderUri(uri.toString())
            val frequency = prefsRepository.preferences.first().autoBackupFrequency
            if (frequency != BackupFrequency.OFF) scheduleAutoBackup(frequency)
        }
    }

    fun setAutoBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            prefsRepository.setAutoBackupFrequency(frequency)
            val folderUri = prefsRepository.preferences.first().autoBackupFolderUri
            if (frequency == BackupFrequency.OFF || folderUri.isEmpty()) {
                cancelAutoBackup()
            } else {
                scheduleAutoBackup(frequency)
            }
        }
    }

    private fun scheduleAutoBackup(frequency: BackupFrequency) {
        val (interval, unit) = when (frequency) {
            BackupFrequency.DAILY   -> 1L to TimeUnit.DAYS
            BackupFrequency.WEEKLY  -> 7L to TimeUnit.DAYS
            BackupFrequency.MONTHLY -> 30L to TimeUnit.DAYS
            BackupFrequency.OFF     -> return
        }
        val request = PeriodicWorkRequestBuilder<BackupWorker>(interval, unit).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            BackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun cancelAutoBackup() {
        WorkManager.getInstance(context).cancelUniqueWork(BackupWorker.WORK_NAME)
    }

    fun setAppRite(rite: AppRite) {
        viewModelScope.launch { prefsRepository.setAppRite(rite) }
    }
}
