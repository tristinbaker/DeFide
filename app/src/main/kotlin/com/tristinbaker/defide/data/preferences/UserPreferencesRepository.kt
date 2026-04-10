package com.tristinbaker.defide.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme { SYSTEM, LIGHT, DARK, AMOLED }
enum class RosaryDiagramStyle { CLASSIC, COMPACT }
enum class RosaryOrder { DOMINICAN, FATIMA }

data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val appLanguage: String = "en",
    val bibleTranslationId: String = "dra",
    val novenaNotificationTime: String = "",   // "HH:MM" or empty = disabled
    val bibleStreakGoal: Int = 1,              // chapters per day to maintain streak
    val bibleLastTranslationId: String = "",
    val bibleLastBookNumber: Int = 0,
    val bibleLastChapter: Int = 0,
    val keepScreenOn: Boolean = false,
    val rosaryDiagramStyle: RosaryDiagramStyle = RosaryDiagramStyle.CLASSIC,
    val rosaryOrder: RosaryOrder = RosaryOrder.DOMINICAN,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_BIBLE_TRANSLATION = stringPreferencesKey("bible_translation")
        private val KEY_NOVENA_NOTIFICATION_TIME = stringPreferencesKey("novena_notification_time")
        private val KEY_BIBLE_STREAK_GOAL = intPreferencesKey("bible_streak_goal")
        private val KEY_BIBLE_LAST_TRANSLATION = stringPreferencesKey("bible_last_translation")
        private val KEY_BIBLE_LAST_BOOK = intPreferencesKey("bible_last_book")
        private val KEY_BIBLE_LAST_CHAPTER = intPreferencesKey("bible_last_chapter")
        private val KEY_KEEP_SCREEN_ON = androidx.datastore.preferences.core.booleanPreferencesKey("keep_screen_on")
        private val KEY_ROSARY_DIAGRAM = stringPreferencesKey("rosary_diagram_style")
        private val KEY_ROSARY_ORDER   = stringPreferencesKey("rosary_order")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            theme = prefs[KEY_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
            appLanguage = prefs[KEY_APP_LANGUAGE] ?: "en",
            bibleTranslationId = prefs[KEY_BIBLE_TRANSLATION] ?: "dra",
            novenaNotificationTime = prefs[KEY_NOVENA_NOTIFICATION_TIME] ?: "",
            bibleStreakGoal = prefs[KEY_BIBLE_STREAK_GOAL] ?: 1,
            bibleLastTranslationId = prefs[KEY_BIBLE_LAST_TRANSLATION] ?: "",
            bibleLastBookNumber = prefs[KEY_BIBLE_LAST_BOOK] ?: 0,
            bibleLastChapter = prefs[KEY_BIBLE_LAST_CHAPTER] ?: 0,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: false,
            rosaryDiagramStyle = prefs[KEY_ROSARY_DIAGRAM]?.let { runCatching { RosaryDiagramStyle.valueOf(it) }.getOrNull() } ?: RosaryDiagramStyle.CLASSIC,
            rosaryOrder = prefs[KEY_ROSARY_ORDER]?.let { runCatching { RosaryOrder.valueOf(it) }.getOrNull() } ?: RosaryOrder.DOMINICAN,
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun setAppLanguage(language: String) {
        dataStore.edit { it[KEY_APP_LANGUAGE] = language }
    }

    suspend fun setBibleTranslation(translationId: String) {
        dataStore.edit { it[KEY_BIBLE_TRANSLATION] = translationId }
    }

    suspend fun setNovenaNotificationTime(time: String) {
        dataStore.edit { it[KEY_NOVENA_NOTIFICATION_TIME] = time }
    }

    suspend fun setBibleStreakGoal(goal: Int) {
        dataStore.edit { it[KEY_BIBLE_STREAK_GOAL] = goal.coerceIn(1, 10) }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setRosaryDiagramStyle(style: RosaryDiagramStyle) {
        dataStore.edit { it[KEY_ROSARY_DIAGRAM] = style.name }
    }

    suspend fun setRosaryOrder(order: RosaryOrder) {
        dataStore.edit { it[KEY_ROSARY_ORDER] = order.name }
    }

    suspend fun setBibleLastPosition(translationId: String, bookNumber: Int, chapter: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BIBLE_LAST_TRANSLATION] = translationId
            prefs[KEY_BIBLE_LAST_BOOK] = bookNumber
            prefs[KEY_BIBLE_LAST_CHAPTER] = chapter
        }
    }
}
