package com.tristinbaker.defide.ui.saints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tristinbaker.defide.data.model.Saint
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import com.tristinbaker.defide.data.repository.SaintsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SaintSortOrder { NAME, FEAST_DATE }

private val MONTH_ORDER = mapOf(
    "january" to 1, "february" to 2, "march" to 3, "april" to 4,
    "may" to 5, "june" to 6, "july" to 7, "august" to 8,
    "september" to 9, "october" to 10, "november" to 11, "december" to 12,
)

private fun Saint.feastSortKey(): Int {
    val parts = (feastDate ?: "").lowercase().split(" ")
    val month = MONTH_ORDER[parts.getOrNull(0)] ?: 0
    val day = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return month * 100 + day
}

@HiltViewModel
class SaintsViewModel @Inject constructor(
    private val repository: SaintsRepository,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _allSaints = MutableStateFlow<List<Saint>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SaintSortOrder.NAME)
    val sortOrder: StateFlow<SaintSortOrder> = _sortOrder.asStateFlow()

    val favoriteIds: StateFlow<Set<String>> = repository.getFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val saints: StateFlow<List<Saint>> = combine(_allSaints, _searchQuery, _sortOrder) { all, query, sort ->
        val filtered = if (query.isBlank()) all
        else all.filter { it.name.contains(query, ignoreCase = true) || it.patronage?.contains(query, ignoreCase = true) == true }
        when (sort) {
            SaintSortOrder.NAME       -> filtered.sortedBy { it.name }
            SaintSortOrder.FEAST_DATE -> filtered.sortedBy { it.feastSortKey() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSaint = MutableStateFlow<Saint?>(null)
    val selectedSaint: StateFlow<Saint?> = _selectedSaint.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepository.preferences
                .distinctUntilChangedBy { it.appLanguage }
                .collect { prefs ->
                    _allSaints.value = repository.getAll(prefs.appLanguage)
                }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SaintSortOrder) {
        _sortOrder.value = order
    }

    fun loadSaint(id: String, language: String) {
        viewModelScope.launch {
            _selectedSaint.value = repository.getById(id, language)
        }
    }

    fun toggleFavorite(saintId: String) {
        viewModelScope.launch { repository.toggleFavorite(saintId) }
    }
}
