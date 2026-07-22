package com.tristinbaker.defide.data.backup

import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.tristinbaker.defide.data.db.user.UserDatabase
import com.tristinbaker.defide.data.preferences.AppFont
import com.tristinbaker.defide.data.preferences.AppTheme
import com.tristinbaker.defide.data.preferences.RosaryOrder
import com.tristinbaker.defide.data.preferences.UserPreferences
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDb: UserDatabase,
    private val prefsRepository: UserPreferencesRepository,
) {
    companion object {
        private val TABLES = listOf(
            "rosary_sessions",
            "bible_bookmarks",
            "bible_highlights",
            "bible_chapter_read",
            "novena_progress",
            "prayer_log",
            "favorite_saints",
            "sin_habits",
        )
    }

    suspend fun exportTo(uri: Uri): Result<Unit> = runCatching {
        val prefs = prefsRepository.preferences.first()
        val json = JSONObject().apply {
            put("version", 4)
            put("prefs", prefsToJson(prefs))
            put("tables", tablesToJson())
        }
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
            writer.write(json.toString(2))
        } ?: error("Could not open output stream")
    }

    suspend fun importFrom(uri: Uri): Result<Unit> = runCatching {
        val jsonStr = context.contentResolver.openInputStream(uri)?.let {
            BufferedReader(InputStreamReader(it)).readText()
        } ?: error("Could not open input stream")
        val json = JSONObject(jsonStr)
        applyPrefs(json.getJSONObject("prefs"))
        applyTables(json.getJSONObject("tables"))
    }

    private fun prefsToJson(prefs: UserPreferences): JSONObject = JSONObject().apply {
        put("theme", prefs.theme.name)
        put("app_font", prefs.appFont.name)
        put("app_language", prefs.appLanguage)
        put("bible_translation_id", prefs.bibleTranslationId)
        put("novena_notification_time", prefs.novenaNotificationTime)
        put("bible_streak_goal", prefs.bibleStreakGoal)
        put("bible_last_translation_id", prefs.bibleLastTranslationId)
        put("bible_last_book_number", prefs.bibleLastBookNumber)
        put("bible_last_chapter", prefs.bibleLastChapter)
        put("keep_screen_on", prefs.keepScreenOn)
        put("full_screen_mode", prefs.fullScreenMode)
        put("rosary_order", prefs.rosaryOrder.name)
        put("rosary_haptic_feedback", prefs.rosaryHapticFeedback)
        put("rosary_narration_enabled", prefs.rosaryNarrationEnabled)
    }

    private suspend fun applyPrefs(json: JSONObject) {
        json.optString("theme").takeIf { it.isNotEmpty() }
            ?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
            ?.let { prefsRepository.setTheme(it) }
        json.optString("app_font").takeIf { it.isNotEmpty() }
            ?.let { runCatching { AppFont.valueOf(it) }.getOrNull() }
            ?.let { prefsRepository.setAppFont(it) }
        json.optString("app_language").takeIf { it.isNotEmpty() }
            ?.let { prefsRepository.setAppLanguage(it) }
        json.optString("bible_translation_id").takeIf { it.isNotEmpty() }
            ?.let { prefsRepository.setBibleTranslation(it) }
        prefsRepository.setNovenaNotificationTime(json.optString("novena_notification_time"))
        prefsRepository.setBibleStreakGoal(json.optInt("bible_streak_goal", 1))
        prefsRepository.setKeepScreenOn(json.optBoolean("keep_screen_on", false))
        prefsRepository.setFullScreenMode(json.optBoolean("full_screen_mode", false))
        json.optString("rosary_order").takeIf { it.isNotEmpty() }
            ?.let { runCatching { RosaryOrder.valueOf(it) }.getOrNull() }
            ?.let { prefsRepository.setRosaryOrder(it) }
        prefsRepository.setRosaryHapticFeedback(json.optBoolean("rosary_haptic_feedback", true))
        prefsRepository.setRosaryNarrationEnabled(json.optBoolean("rosary_narration_enabled", false))
        val lastBook = json.optInt("bible_last_book_number", 0)
        if (lastBook > 0) {
            prefsRepository.setBibleLastPosition(
                json.optString("bible_last_translation_id"),
                lastBook,
                json.optInt("bible_last_chapter", 0),
            )
        }
    }

    private fun tablesToJson(): JSONObject = JSONObject().also { tablesJson ->
        val db = userDb.openHelper.readableDatabase
        for (tableName in TABLES) {
            val rows = JSONArray()
            db.query("SELECT * FROM `$tableName`").use { cursor ->
                val colNames = cursor.columnNames
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    colNames.forEachIndexed { i, name ->
                        when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_INTEGER -> row.put(name, cursor.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT   -> row.put(name, cursor.getDouble(i))
                            Cursor.FIELD_TYPE_NULL    -> row.put(name, JSONObject.NULL)
                            else                      -> row.put(name, cursor.getString(i))
                        }
                    }
                    rows.put(row)
                }
            }
            tablesJson.put(tableName, rows)
        }
    }

    private fun applyTables(json: JSONObject) {
        val db = userDb.openHelper.writableDatabase
        db.beginTransaction()
        try {
            for (tableName in TABLES) {
                db.execSQL("DELETE FROM `$tableName`")
                val rows = json.optJSONArray(tableName) ?: continue
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val keys = row.keys().asSequence().toList()
                    val colList = keys.joinToString(", ") { "`$it`" }
                    val placeholders = keys.joinToString(", ") { "?" }
                    val values = keys.map { key ->
                        val v = row.get(key)
                        if (v == JSONObject.NULL) null else v.toString()
                    }.toTypedArray()
                    db.execSQL(
                        "INSERT OR REPLACE INTO `$tableName` ($colList) VALUES ($placeholders)",
                        values,
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
