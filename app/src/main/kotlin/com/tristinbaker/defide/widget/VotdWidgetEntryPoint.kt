package com.tristinbaker.defide.widget

import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import com.tristinbaker.defide.data.repository.BibleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface VotdWidgetEntryPoint {
    fun bibleRepository(): BibleRepository
    fun prefsRepository(): UserPreferencesRepository
}
