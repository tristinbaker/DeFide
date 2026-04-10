package com.tristinbaker.defide.data.db.content

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val DB_NAME = "defide_content.db"
private const val ASSET_PATH = "databases/$DB_NAME"
// Bump this whenever the compiled content DB changes so the new asset is re-copied on next launch.
private const val CONTENT_VERSION = 23

/**
 * Wraps the pre-populated, read-only content SQLite database.
 * Copies from assets on first launch or whenever CONTENT_VERSION is bumped.
 * Bypasses Room to avoid schema validation conflicts with FTS virtual tables.
 */
@Singleton
class ContentDatabase @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val db: SQLiteDatabase by lazy {
        val dest = context.getDatabasePath(DB_NAME)
        val versionFile = File(dest.parent, "${DB_NAME}.version")
        val storedVersion = if (versionFile.exists()) versionFile.readText().trim().toIntOrNull() ?: 0 else 0

        if (!dest.exists() || storedVersion < CONTENT_VERSION) {
            copyFromAssets(dest)
            versionFile.writeText(CONTENT_VERSION.toString())
        }

        SQLiteDatabase.openDatabase(dest.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun copyFromAssets(dest: File) {
        dest.parentFile?.mkdirs()
        context.assets.open(ASSET_PATH).use { input ->
            dest.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}
