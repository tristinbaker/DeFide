package app.defide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.defide.data.preferences.UserPreferencesRepository
import app.defide.ui.DeFideApp
import app.defide.ui.theme.DeFideTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs by prefsRepository.preferences.collectAsState(initial = app.defide.data.preferences.UserPreferences())
            DeFideTheme(theme = prefs.theme) {
                DeFideApp()
            }
        }
    }
}
