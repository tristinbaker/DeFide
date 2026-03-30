package com.tristinbaker.defide.ui.rosary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tristinbaker.defide.data.model.Mystery
import com.tristinbaker.defide.data.model.MysteryBead
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import com.tristinbaker.defide.data.repository.PrayerRepository
import com.tristinbaker.defide.data.repository.RosaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/** Returns the traditional mystery ID for a given day of the week. */
fun suggestedMysteryId(date: LocalDate = LocalDate.now()): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY, DayOfWeek.SATURDAY -> "joyful"
    DayOfWeek.TUESDAY, DayOfWeek.FRIDAY  -> "sorrowful"
    DayOfWeek.THURSDAY                   -> "luminous"
    else /* WEDNESDAY, SUNDAY */         -> "glorious"
}

@HiltViewModel
class RosaryViewModel @Inject constructor(
    private val repository: RosaryRepository,
    private val prayerRepository: PrayerRepository,
    private val prefsRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _mysteries = MutableStateFlow<List<Mystery>>(emptyList())
    val mysteries: StateFlow<List<Mystery>> = _mysteries.asStateFlow()

    val todaysMysteryId: String = suggestedMysteryId()

    private val _beads = MutableStateFlow<List<MysteryBead>>(emptyList())
    val beads: StateFlow<List<MysteryBead>> = _beads.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    // prayerId -> body text
    private val _prayerTexts = MutableStateFlow<Map<String, String>>(emptyMap())
    val prayerTexts: StateFlow<Map<String, String>> = _prayerTexts.asStateFlow()

    // prayerId -> title text
    private val _prayerTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val prayerTitles: StateFlow<Map<String, String>> = _prayerTitles.asStateFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    private var currentLanguage = "en"

    init {
        viewModelScope.launch {
            currentLanguage = prefsRepository.preferences.first().appLanguage
            _mysteries.value = repository.getMysteries(currentLanguage)
            val prayers = prayerRepository.getAll(currentLanguage)
            _prayerTexts.value = prayers.associate { it.id to it.body }
            _prayerTitles.value = prayers.associate { it.id to it.title }
        }
    }

    fun startSession(mysteryId: String) {
        if (_beads.value.isNotEmpty()) return  // Already started — preserve position
        viewModelScope.launch {
            val lang = prefsRepository.preferences.first().appLanguage
            _beads.value = repository.getBeads(mysteryId, lang)
            _currentPosition.value = 0
            _sessionId.value = repository.startSession(mysteryId)
        }
    }

    fun advance() {
        val next = _currentPosition.value + 1
        if (next < _beads.value.size) {
            _currentPosition.value = next
        }
    }

    fun back() {
        val prev = _currentPosition.value - 1
        if (prev >= 0) _currentPosition.value = prev
    }

    fun completeSession(onDone: () -> Unit) {
        viewModelScope.launch {
            _sessionId.value?.let { repository.completeSession(it) }
            onDone()
        }
    }
}
