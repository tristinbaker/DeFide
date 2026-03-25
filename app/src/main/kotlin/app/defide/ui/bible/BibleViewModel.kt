package app.defide.ui.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.defide.data.db.user.entity.BibleHighlightEntity
import app.defide.data.model.Book
import app.defide.data.model.Translation
import app.defide.data.model.Verse
import app.defide.data.preferences.UserPreferencesRepository
import app.defide.data.repository.BibleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BibleViewModel @Inject constructor(
    private val repository: BibleRepository,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _translations = MutableStateFlow<List<Translation>>(emptyList())
    val translations: StateFlow<List<Translation>> = _translations.asStateFlow()

    private val _selectedTranslationId = MutableStateFlow("dra")
    val selectedTranslationId: StateFlow<String> = _selectedTranslationId.asStateFlow()

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _chapterCount = MutableStateFlow(0)
    val chapterCount: StateFlow<Int> = _chapterCount.asStateFlow()

    // (translationId, bookNumber) key driving the read-chapters flow
    private val _readKey = MutableStateFlow<Pair<String, Int>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val readChapters: StateFlow<Set<Int>> = _readKey
        .flatMapLatest { key ->
            if (key == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.getReadChapters(key.first, key.second)
        }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _verses = MutableStateFlow<List<Verse>>(emptyList())
    val verses: StateFlow<List<Verse>> = _verses.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Verse>>(emptyList())
    val searchResults: StateFlow<List<Verse>> = _searchResults.asStateFlow()

    // verseId -> color, live from DB
    val highlights: StateFlow<Map<Int, String>> = repository.getAllHighlights()
        .map { list -> list.associate { it.verseId to it.color } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val bookmarks = repository.getBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _translations.value = repository.getTranslations()
        }
        // React to translation preference changes (covers initial load + Settings changes)
        viewModelScope.launch {
            prefsRepository.preferences
                .map { it.bibleTranslationId }
                .distinctUntilChanged()
                .collect { id ->
                    _selectedTranslationId.value = id
                    _books.value = repository.getBooks(id)
                }
        }
    }

    fun loadChapterCount(bookId: Int) {
        viewModelScope.launch {
            _chapterCount.value = repository.getChapterCount(bookId)
        }
    }

    fun loadReadChapters(translationId: String, bookNumber: Int) {
        _readKey.value = translationId to bookNumber
    }

    fun markChapterRead(translationId: String, bookNumber: Int, chapter: Int) {
        viewModelScope.launch { repository.markChapterRead(translationId, bookNumber, chapter) }
    }

    fun unmarkChapterRead(translationId: String, bookNumber: Int, chapter: Int) {
        viewModelScope.launch { repository.unmarkChapterRead(translationId, bookNumber, chapter) }
    }

    fun resetBookProgress(translationId: String, bookNumber: Int) {
        viewModelScope.launch { repository.resetBookProgress(translationId, bookNumber) }
    }

    fun loadVerses(bookId: Int, chapter: Int) {
        viewModelScope.launch {
            _verses.value = repository.getVerses(bookId, chapter)
            _chapterCount.value = repository.getChapterCount(bookId)
        }
    }

    fun search(query: String) {
        if (query.isBlank()) { _searchResults.value = emptyList(); return }
        viewModelScope.launch {
            _searchResults.value = repository.search(_selectedTranslationId.value, query)
        }
    }

    fun deleteBookmark(id: String) {
        viewModelScope.launch { repository.deleteBookmark(id) }
    }

    fun addBookmark(bookNumber: Int, chapter: Int, verse: Int) {
        viewModelScope.launch {
            repository.addBookmark(
                translationId = _selectedTranslationId.value,
                bookNumber = bookNumber,
                chapter = chapter,
                verse = verse,
            )
        }
    }

    fun addHighlight(verseId: Int, color: String) {
        viewModelScope.launch { repository.addHighlight(verseId, color) }
    }

    fun removeHighlight(verseId: Int) {
        viewModelScope.launch { repository.removeHighlight(verseId) }
    }
}
