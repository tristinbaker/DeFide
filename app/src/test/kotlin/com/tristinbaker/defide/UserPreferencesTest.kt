package com.tristinbaker.defide

import com.tristinbaker.defide.data.preferences.AppTheme
import com.tristinbaker.defide.data.preferences.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesTest {

    @Test
    fun `default UserPreferences has SYSTEM theme`() {
        val prefs = UserPreferences()
        assertEquals(AppTheme.SYSTEM, prefs.theme)
    }

    @Test
    fun `default UserPreferences has dra translation`() {
        val prefs = UserPreferences()
        assertEquals("dra", prefs.bibleTranslationId)
    }

    @Test
    fun `default UserPreferences has empty notification time (disabled)`() {
        val prefs = UserPreferences()
        assertEquals("", prefs.novenaNotificationTime)
    }

    @Test
    fun `AppTheme has exactly three options`() {
        assertEquals(3, AppTheme.entries.size)
    }

    @Test
    fun `AppTheme entries are SYSTEM LIGHT DARK`() {
        val names = AppTheme.entries.map { it.name }
        assertEquals(listOf("SYSTEM", "LIGHT", "DARK"), names)
    }

    @Test
    fun `UserPreferences copy preserves unmodified fields`() {
        val original = UserPreferences(theme = AppTheme.DARK, bibleTranslationId = "vulgate")
        val updated = original.copy(theme = AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, updated.theme)
        assertEquals("vulgate", updated.bibleTranslationId)
        assertEquals("", updated.novenaNotificationTime)
    }
}
