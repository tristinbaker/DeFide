package app.defide.ui.catechism

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.defide.data.model.CccSection
import app.defide.data.repository.CatechismRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CatechismViewModel @Inject constructor(
    private val repository: CatechismRepository,
) : ViewModel() {

    private val _parts = MutableStateFlow<List<Int>>(emptyList())
    val parts: StateFlow<List<Int>> = _parts.asStateFlow()

    private val _sections = MutableStateFlow<List<CccSection>>(emptyList())
    val sections: StateFlow<List<CccSection>> = _sections.asStateFlow()

    private val _detail = MutableStateFlow<CccSection?>(null)
    val detail: StateFlow<CccSection?> = _detail.asStateFlow()

    private val _prevSectionId = MutableStateFlow<Int?>(null)
    val prevSectionId: StateFlow<Int?> = _prevSectionId.asStateFlow()

    private val _nextSectionId = MutableStateFlow<Int?>(null)
    val nextSectionId: StateFlow<Int?> = _nextSectionId.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CccSection>>(emptyList())
    val searchResults: StateFlow<List<CccSection>> = _searchResults.asStateFlow()

    init {
        viewModelScope.launch {
            _parts.value = repository.getParts()
            // If no hierarchy data, pre-load the flat browse list
            if (_parts.value.isEmpty()) {
                _sections.value = repository.getAll(limit = 50, offset = 0)
            }
        }
    }

    fun loadMore(offset: Int) {
        viewModelScope.launch {
            val more = repository.getAll(limit = 50, offset = offset)
            _sections.value = _sections.value + more
        }
    }

    fun loadSections(part: Int) {
        viewModelScope.launch { _sections.value = repository.getSectionsByPart(part) }
    }

    fun loadDetail(id: Int) {
        viewModelScope.launch {
            _detail.value = repository.getById(id)
            _prevSectionId.value = repository.getPrevId(id)
            _nextSectionId.value = repository.getNextId(id)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch { _searchResults.value = repository.search(query) }
    }
}
