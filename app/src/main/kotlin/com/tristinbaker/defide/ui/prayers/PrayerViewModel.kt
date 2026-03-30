package com.tristinbaker.defide.ui.prayers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tristinbaker.defide.data.model.Prayer
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import com.tristinbaker.defide.data.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrayerViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _results = MutableStateFlow<List<Prayer>>(emptyList())
    val results: StateFlow<List<Prayer>> = _results.asStateFlow()

    private val _detail = MutableStateFlow<Prayer?>(null)
    val detail: StateFlow<Prayer?> = _detail.asStateFlow()

    private var currentLanguage: String = "en"

    init {
        viewModelScope.launch {
            prefsRepository.preferences
                .map { it.appLanguage }
                .distinctUntilChanged()
                .collect { lang ->
                    currentLanguage = lang
                    _tags.value = repository.getTags(lang)
                    _results.value = repository.getAll(lang)
                }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _results.value = if (query.isBlank()) repository.getAll(currentLanguage)
                             else repository.search(query, currentLanguage)
        }
    }

    fun filterByTag(tag: String?) {
        viewModelScope.launch {
            _results.value = if (tag == null) repository.getAll(currentLanguage)
                             else repository.getByTag(tag, currentLanguage)
        }
    }

    fun loadDetail(id: String) {
        viewModelScope.launch {
            val lang = prefsRepository.preferences.first().appLanguage
            _detail.value = repository.getById(id, lang)
        }
    }

    fun logPrayer(id: String) {
        viewModelScope.launch { repository.logPrayer(id) }
    }
}
