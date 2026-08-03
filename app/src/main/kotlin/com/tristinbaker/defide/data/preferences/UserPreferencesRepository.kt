package com.tristinbaker.defide.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme { SYSTEM, LIGHT, DARK, AMOLED, DYNAMIC }
enum class AppFont { SERIF, SYSTEM, SANS_SERIF }
enum class RosaryOrder { DOMINICAN, FATIMA }
enum class BackupFrequency { OFF, DAILY, WEEKLY, MONTHLY }
enum class AppRite { MODERN, TRADITIONAL, LATIN }

val AppRite.language: String
    get() = when (this) {
        AppRite.MODERN      -> "en"
        AppRite.TRADITIONAL -> "en"
        AppRite.LATIN       -> "la"
    }

/** Language for all prayer/mystery content — differs from `language` in TRADITIONAL mode. */
val AppRite.contentLanguage: String
    get() = when (this) {
        AppRite.MODERN      -> "en"
        AppRite.TRADITIONAL -> "la"
        AppRite.LATIN       -> "la"
    }

val AppRite.displayNameResId: Int
    @Composable get() = when (this) {
        AppRite.MODERN      -> com.tristinbaker.defide.R.string.rite_modern
        AppRite.TRADITIONAL -> com.tristinbaker.defide.R.string.rite_traditional
        AppRite.LATIN       -> com.tristinbaker.defide.R.string.rite_latin
    }

data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val appFont: AppFont = AppFont.SERIF,
    val appLanguage: String = "en",
    val bibleTranslationId: String = "dra",
    val novenaNotificationTime: String = "",   // "HH:MM" or empty = disabled
    val rosaryNotificationTime: String = "",   // "HH:MM" or empty = disabled
    val bibleStreakGoal: Int = 1,              // chapters per day to maintain streak
    val bibleLastTranslationId: String = "",
    val bibleLastBookNumber: Int = 0,
    val bibleLastChapter: Int = 0,
    val keepScreenOn: Boolean = false,
    val fullScreenMode: Boolean = false,
    val rosaryOrder: RosaryOrder = RosaryOrder.DOMINICAN,
    val rosaryHapticFeedback: Boolean = true,
    val rosaryNarrationEnabled: Boolean = false,
    val autoBackupFrequency: BackupFrequency = BackupFrequency.OFF,
    val autoBackupFolderUri: String = "",
    val rosaryIntentions: List<String> = List(5) { "" },
    val rosaryIntentionInDesign: Boolean = false,
    val appRite: AppRite = AppRite.MODERN,
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_APP_FONT = stringPreferencesKey("app_font")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_BIBLE_TRANSLATION = stringPreferencesKey("bible_translation")
        private val KEY_NOVENA_NOTIFICATION_TIME = stringPreferencesKey("novena_notification_time")
        private val KEY_ROSARY_NOTIFICATION_TIME = stringPreferencesKey("rosary_notification_time")
        private val KEY_BIBLE_STREAK_GOAL = intPreferencesKey("bible_streak_goal")
        private val KEY_BIBLE_LAST_TRANSLATION = stringPreferencesKey("bible_last_translation")
        private val KEY_BIBLE_LAST_BOOK = intPreferencesKey("bible_last_book")
        private val KEY_BIBLE_LAST_CHAPTER = intPreferencesKey("bible_last_chapter")
        private val KEY_KEEP_SCREEN_ON = androidx.datastore.preferences.core.booleanPreferencesKey("keep_screen_on")
        private val KEY_FULL_SCREEN_MODE = androidx.datastore.preferences.core.booleanPreferencesKey("full_screen_mode")
        private val KEY_ROSARY_ORDER     = stringPreferencesKey("rosary_order")
        private val KEY_ROSARY_HAPTIC    = androidx.datastore.preferences.core.booleanPreferencesKey("rosary_haptic_feedback")
        private val KEY_ROSARY_NARRATION = androidx.datastore.preferences.core.booleanPreferencesKey("rosary_narration_enabled")
        private val KEY_AUTO_BACKUP_FREQUENCY = stringPreferencesKey("auto_backup_frequency")
        private val KEY_AUTO_BACKUP_FOLDER_URI = stringPreferencesKey("auto_backup_folder_uri")
        private val KEY_ROSARY_INTENTION_0 = stringPreferencesKey("rosary_intention_0")
        private val KEY_ROSARY_INTENTION_1 = stringPreferencesKey("rosary_intention_1")
        private val KEY_ROSARY_INTENTION_2 = stringPreferencesKey("rosary_intention_2")
        private val KEY_ROSARY_INTENTION_3 = stringPreferencesKey("rosary_intention_3")
        private val KEY_ROSARY_INTENTION_4 = stringPreferencesKey("rosary_intention_4")
        private val KEY_ROSARY_INTENTION_IN_DESIGN = androidx.datastore.preferences.core.booleanPreferencesKey("rosary_intention_in_design")
        private val KEY_APP_RITE         = stringPreferencesKey("app_rite")
    }

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val storedRite = prefs[KEY_APP_RITE]?.let { runCatching { AppRite.valueOf(it) }.getOrNull() } ?: AppRite.MODERN
        UserPreferences(
            theme = prefs[KEY_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
            appFont = prefs[KEY_APP_FONT]?.let { runCatching { AppFont.valueOf(it) }.getOrNull() } ?: AppFont.SERIF,
            appLanguage = prefs[KEY_APP_LANGUAGE] ?: storedRite.language,
            bibleTranslationId = prefs[KEY_BIBLE_TRANSLATION] ?: "dra",
            novenaNotificationTime = prefs[KEY_NOVENA_NOTIFICATION_TIME] ?: "",
            rosaryNotificationTime = prefs[KEY_ROSARY_NOTIFICATION_TIME] ?: "",
            bibleStreakGoal = prefs[KEY_BIBLE_STREAK_GOAL] ?: 1,
            bibleLastTranslationId = prefs[KEY_BIBLE_LAST_TRANSLATION] ?: "",
            bibleLastBookNumber = prefs[KEY_BIBLE_LAST_BOOK] ?: 0,
            bibleLastChapter = prefs[KEY_BIBLE_LAST_CHAPTER] ?: 0,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: false,
            fullScreenMode = prefs[KEY_FULL_SCREEN_MODE] ?: false,
            rosaryOrder = prefs[KEY_ROSARY_ORDER]?.let { runCatching { RosaryOrder.valueOf(it) }.getOrNull() } ?: RosaryOrder.DOMINICAN,
            rosaryHapticFeedback = prefs[KEY_ROSARY_HAPTIC] ?: true,
            rosaryNarrationEnabled = prefs[KEY_ROSARY_NARRATION] ?: false,
            autoBackupFrequency = prefs[KEY_AUTO_BACKUP_FREQUENCY]?.let { runCatching { BackupFrequency.valueOf(it) }.getOrNull() } ?: BackupFrequency.OFF,
            autoBackupFolderUri = prefs[KEY_AUTO_BACKUP_FOLDER_URI] ?: "",
            rosaryIntentions = listOf(
                prefs[KEY_ROSARY_INTENTION_0] ?: "",
                prefs[KEY_ROSARY_INTENTION_1] ?: "",
                prefs[KEY_ROSARY_INTENTION_2] ?: "",
                prefs[KEY_ROSARY_INTENTION_3] ?: "",
                prefs[KEY_ROSARY_INTENTION_4] ?: "",
            ),
            rosaryIntentionInDesign = prefs[KEY_ROSARY_INTENTION_IN_DESIGN] ?: false,
            appRite = prefs[KEY_APP_RITE]?.let { runCatching { AppRite.valueOf(it) }.getOrNull() } ?: AppRite.MODERN,
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun setAppFont(font: AppFont) {
        dataStore.edit { it[KEY_APP_FONT] = font.name }
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

    suspend fun setRosaryNotificationTime(time: String) {
        dataStore.edit { it[KEY_ROSARY_NOTIFICATION_TIME] = time }
    }

    suspend fun setRosaryIntentionInDesign(enabled: Boolean) {
        dataStore.edit { it[KEY_ROSARY_INTENTION_IN_DESIGN] = enabled }
    }

    suspend fun setBibleStreakGoal(goal: Int) {
        dataStore.edit { it[KEY_BIBLE_STREAK_GOAL] = goal.coerceIn(1, 10) }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setFullScreenMode(enabled: Boolean) {
        dataStore.edit { it[KEY_FULL_SCREEN_MODE] = enabled }
    }

    suspend fun setRosaryOrder(order: RosaryOrder) {
        dataStore.edit { it[KEY_ROSARY_ORDER] = order.name }
    }

    suspend fun setRosaryHapticFeedback(enabled: Boolean) {
        dataStore.edit { it[KEY_ROSARY_HAPTIC] = enabled }
    }

    suspend fun setRosaryNarrationEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ROSARY_NARRATION] = enabled }
    }

    suspend fun setAutoBackupFrequency(frequency: BackupFrequency) {
        dataStore.edit { it[KEY_AUTO_BACKUP_FREQUENCY] = frequency.name }
    }

    suspend fun setAutoBackupFolderUri(uri: String) {
        dataStore.edit { it[KEY_AUTO_BACKUP_FOLDER_URI] = uri }
    }

    suspend fun setAppRite(rite: AppRite) {
        dataStore.edit { prefs ->
            prefs[KEY_APP_RITE] = rite.name
            prefs[KEY_APP_LANGUAGE] = rite.language
        }
    }

    suspend fun setBibleLastPosition(translationId: String, bookNumber: Int, chapter: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BIBLE_LAST_TRANSLATION] = translationId
            prefs[KEY_BIBLE_LAST_BOOK] = bookNumber
            prefs[KEY_BIBLE_LAST_CHAPTER] = chapter
        }
    }

    suspend fun setRosaryIntentions(intentions: List<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_ROSARY_INTENTION_0] = intentions.getOrElse(0) { "" }
            prefs[KEY_ROSARY_INTENTION_1] = intentions.getOrElse(1) { "" }
            prefs[KEY_ROSARY_INTENTION_2] = intentions.getOrElse(2) { "" }
            prefs[KEY_ROSARY_INTENTION_3] = intentions.getOrElse(3) { "" }
            prefs[KEY_ROSARY_INTENTION_4] = intentions.getOrElse(4) { "" }
        }
    }

    suspend fun clearRosaryIntentions() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_ROSARY_INTENTION_0)
            prefs.remove(KEY_ROSARY_INTENTION_1)
            prefs.remove(KEY_ROSARY_INTENTION_2)
            prefs.remove(KEY_ROSARY_INTENTION_3)
            prefs.remove(KEY_ROSARY_INTENTION_4)
        }
    }
}
