package app.defide.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.defide.data.preferences.UserPreferencesRepository
import app.defide.data.repository.BibleRepository
import app.defide.data.repository.RosaryRepository
import app.defide.ui.rosary.suggestedMysteryId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VerseOfDay(
    val text: String,
    val reference: String,
    val translationId: String,
    val bookNumber: Int,
    val chapter: Int,
    val verse: Int,
)
data class TodaysMystery(val id: String, val name: String, val traditionalDays: String?)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bibleRepository: BibleRepository,
    private val rosaryRepository: RosaryRepository,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _verseOfDay = MutableStateFlow<VerseOfDay?>(null)
    val verseOfDay: StateFlow<VerseOfDay?> = _verseOfDay.asStateFlow()

    private val _todaysMystery = MutableStateFlow<TodaysMystery?>(null)
    val todaysMystery: StateFlow<TodaysMystery?> = _todaysMystery.asStateFlow()

    init {
        viewModelScope.launch {
            val translationId = prefsRepository.preferences.first().bibleTranslationId
            bibleRepository.getVerseOfDay(translationId)?.let { (verse, book) ->
                _verseOfDay.value = VerseOfDay(
                    text = verse.text,
                    reference = "${book.fullName} ${verse.chapter}:${verse.verse}",
                    translationId = translationId,
                    bookNumber = book.bookNumber,
                    chapter = verse.chapter,
                    verse = verse.verse,
                )
            }
        }
        viewModelScope.launch {
            val mysteryId = suggestedMysteryId()
            rosaryRepository.getMysteries().find { it.id == mysteryId }?.let {
                _todaysMystery.value = TodaysMystery(it.id, it.name, it.traditionalDays)
            }
        }
    }
}
