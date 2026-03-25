package app.defide.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val bibleTranslationId: String = "dra",
    val novenaNotificationTime: String = "",   // "HH:MM" or empty = disabled
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_BIBLE_TRANSLATION = stringPreferencesKey("bible_translation")
        private val KEY_NOVENA_NOTIFICATION_TIME = stringPreferencesKey("novena_notification_time")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            theme = prefs[KEY_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
            bibleTranslationId = prefs[KEY_BIBLE_TRANSLATION] ?: "dra",
            novenaNotificationTime = prefs[KEY_NOVENA_NOTIFICATION_TIME] ?: "",
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun setBibleTranslation(translationId: String) {
        dataStore.edit { it[KEY_BIBLE_TRANSLATION] = translationId }
    }

    suspend fun setNovenaNotificationTime(time: String) {
        dataStore.edit { it[KEY_NOVENA_NOTIFICATION_TIME] = time }
    }
}
