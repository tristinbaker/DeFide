package app.defide.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.defide.data.preferences.AppTheme
import app.defide.data.preferences.UserPreferences
import app.defide.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = prefsRepository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { prefsRepository.setTheme(theme) }
    }

    fun setBibleTranslation(translationId: String) {
        viewModelScope.launch { prefsRepository.setBibleTranslation(translationId) }
    }

    fun setNovenaNotificationTime(time: String) {
        viewModelScope.launch { prefsRepository.setNovenaNotificationTime(time) }
    }
}
