package com.tristinbaker.defide.ui.prayers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tristinbaker.defide.data.model.Prayer
import com.tristinbaker.defide.data.repository.PrayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrayerViewModel @Inject constructor(
    private val repository: PrayerRepository,
) : ViewModel() {

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _results = MutableStateFlow<List<Prayer>>(emptyList())
    val results: StateFlow<List<Prayer>> = _results.asStateFlow()

    private val _detail = MutableStateFlow<Prayer?>(null)
    val detail: StateFlow<Prayer?> = _detail.asStateFlow()

    init {
        viewModelScope.launch {
            _tags.value = repository.getTags()
            _results.value = repository.getAll()
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _results.value = if (query.isBlank()) repository.getAll() else repository.search(query)
        }
    }

    fun filterByTag(tag: String?) {
        viewModelScope.launch {
            _results.value = if (tag == null) repository.getAll() else repository.getByTag(tag)
        }
    }

    fun loadDetail(id: String) {
        viewModelScope.launch { _detail.value = repository.getById(id) }
    }

    fun logPrayer(id: String) {
        viewModelScope.launch { repository.logPrayer(id) }
    }
}
