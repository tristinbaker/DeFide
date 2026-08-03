package com.tristinbaker.defide.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tristinbaker.defide.data.preferences.AppRite
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import com.tristinbaker.defide.data.preferences.contentLanguage
import com.tristinbaker.defide.data.preferences.language
import com.tristinbaker.defide.data.repository.BibleRepository
import com.tristinbaker.defide.data.repository.RosaryRepository
import com.tristinbaker.defide.data.repository.SaintsRepository
import com.tristinbaker.defide.data.repository.SinHabitRepository
import com.tristinbaker.defide.ui.rosary.suggestedMysteryId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
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
data class SaintOfDay(val id: String, val name: String, val feastDate: String?)
data class SinHabitUi(val id: String, val name: String, val streak: Int)

private val ENGLISH_MONTHS = mapOf(
    "january" to 1, "february" to 2, "march" to 3, "april" to 4,
    "may" to 5, "june" to 6, "july" to 7, "august" to 8,
    "september" to 9, "october" to 10, "november" to 11, "december" to 12,
)

/** Parses "September 29" or "August 15 (Assumption)" into month/day; null if unparseable. */
private fun parseFeastMonthDay(feastDate: String?): Pair<Int, Int>? {
    val parts = (feastDate ?: "").split(" ")
    val month = ENGLISH_MONTHS[parts.getOrNull(0)?.lowercase()] ?: return null
    val day = parts.getOrNull(1)?.takeWhile { it.isDigit() }?.toIntOrNull() ?: return null
    return month to day
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bibleRepository: BibleRepository,
    private val rosaryRepository: RosaryRepository,
    private val saintsRepository: SaintsRepository,
    private val sinHabitRepository: SinHabitRepository,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _verseOfDay = MutableStateFlow<VerseOfDay?>(null)
    val verseOfDay: StateFlow<VerseOfDay?> = _verseOfDay.asStateFlow()

    private val _todaysMystery = MutableStateFlow<TodaysMystery?>(null)
    val todaysMystery: StateFlow<TodaysMystery?> = _todaysMystery.asStateFlow()

    private val _saintOfDay = MutableStateFlow<SaintOfDay?>(null)
    val saintOfDay: StateFlow<SaintOfDay?> = _saintOfDay.asStateFlow()

    val appLanguage: StateFlow<String> = prefsRepository.preferences
        .map { it.appLanguage }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val bibleStreak: StateFlow<Int> = combine(
        bibleRepository.getAllReadHistory(),
        prefsRepository.preferences.map { it.bibleStreakGoal },
    ) { history, goal ->
        BibleRepository.computeBibleStreak(history, goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val rosaryStreak: StateFlow<Int> = rosaryRepository.getCompletedSessions()
        .map { RosaryRepository.computeRosaryStreak(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sinHabits: StateFlow<List<SinHabitUi>> = sinHabitRepository.getAll()
        .map { habits ->
            habits.map { SinHabitUi(it.id, it.name, SinHabitRepository.computeStreak(it)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSinHabit(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { sinHabitRepository.addHabit(trimmed) }
    }

    fun logSinRelapse(id: String) {
        viewModelScope.launch { sinHabitRepository.logRelapse(id) }
    }

    fun removeSinHabit(id: String) {
        viewModelScope.launch { sinHabitRepository.removeHabit(id) }
    }

    init {
        viewModelScope.launch {
            combine(
                prefsRepository.preferences,
                prefsRepository.preferences.map { it.appRite }.distinctUntilChanged(),
            ) { prefs, rite ->
                Pair(prefs, rite)
            }.collect { (prefs, rite) ->
                // Use Latin Vulgate in LATIN/Traditional (bible in English in Traditional)
                val translationId = when (rite) {
                    AppRite.LATIN       -> "vulgate"
                    AppRite.TRADITIONAL -> "dra"
                    AppRite.MODERN     -> prefs.bibleTranslationId
                }
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
        }
        viewModelScope.launch {
            val today = LocalDate.now()
            prefsRepository.preferences
                .distinctUntilChangedBy { it.appLanguage }
                .collect { prefs ->
                    // Feast dates are only reliably English in the "en" saints data
                    // (Italian ships localized dates), so match against English and
                    // fetch the localized record by id for display.
                    val todaysSaint = saintsRepository.getAll("en")
                        .filter { saint ->
                            parseFeastMonthDay(saint.feastDate)
                                ?.let { (m, d) -> m == today.monthValue && d == today.dayOfMonth } == true
                        }
                        .minWithOrNull(compareBy(nullsLast()) { it.rank })
                    _saintOfDay.value = todaysSaint?.let { s ->
                        val localized = if (prefs.appLanguage == "en") s
                        else saintsRepository.getById(s.id, prefs.appLanguage) ?: s
                        SaintOfDay(localized.id, localized.name, localized.feastDate)
                    }
                }
        }
        viewModelScope.launch {
            val mysteryId = suggestedMysteryId()
            prefsRepository.preferences
                .distinctUntilChangedBy { it.appRite to it.appLanguage }
                .collect { prefs ->
                    // MODERN rite uses the user's appLanguage; TRADITIONAL/LATIN use a fixed rite language
                    val mysteryLang = if (prefs.appRite == AppRite.MODERN) prefs.appLanguage else prefs.appRite.language
                    rosaryRepository.getMysteries(mysteryLang).find { it.id == mysteryId }?.let {
                        _todaysMystery.value = TodaysMystery(it.id, it.name, it.traditionalDays)
                    }
                }
        }
    }
}
