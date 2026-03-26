package com.tristinbaker.defide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import com.tristinbaker.defide.ui.DeFideApp
import com.tristinbaker.defide.ui.theme.DeFideTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by prefsRepository.preferences.collectAsState(initial = com.tristinbaker.defide.data.preferences.UserPreferences())
            DeFideTheme(theme = prefs.theme) {
                DeFideApp()
            }
        }
    }
}
