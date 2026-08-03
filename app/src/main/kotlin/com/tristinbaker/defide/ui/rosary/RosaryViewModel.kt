package com.tristinbaker.defide.ui.rosary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tristinbaker.defide.data.model.Mystery
import com.tristinbaker.defide.data.model.MysteryBead
import com.tristinbaker.defide.data.preferences.AppRite
import com.tristinbaker.defide.data.preferences.RosaryOrder
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import com.tristinbaker.defide.data.preferences.contentLanguage
import com.tristinbaker.defide.data.preferences.language
import com.tristinbaker.defide.data.repository.PrayerRepository
import com.tristinbaker.defide.data.repository.RosaryRepository
import com.tristinbaker.defide.data.tts.TtsController
import com.tristinbaker.defide.data.tts.TtsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val ttsController: TtsController,
) : ViewModel() {

    private val _mysteries = MutableStateFlow<List<Mystery>>(emptyList())
    val mysteries: StateFlow<List<Mystery>> = _mysteries.asStateFlow()

    /** English mysteries — used for mystery titles/scripture in Traditional mode. */
    private val _englishMysteries = MutableStateFlow<List<Mystery>>(emptyList())
    val englishMysteries: StateFlow<List<Mystery>> = _englishMysteries.asStateFlow()

    /** Currently active mystery ID (set when a session starts). */
    private val _currentMysteryId = MutableStateFlow<String?>(null)
    val currentMysteryId: StateFlow<String?> = _currentMysteryId.asStateFlow()

    val todaysMysteryId: String = suggestedMysteryId()

    private val _beads = MutableStateFlow<List<MysteryBead>>(emptyList())
    val beads: StateFlow<List<MysteryBead>> = _beads.asStateFlow()

    /**
     * English beads — cached per mystery ID.
     * Used in Traditional mode to display scripture/meditation in English
     * while prayers remain in Latin.
     */
    private val _englishBeads = MutableStateFlow<Map<String, List<MysteryBead>>>(emptyMap())
    val englishBeads: StateFlow<Map<String, List<MysteryBead>>> = _englishBeads.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _prayerTexts = MutableStateFlow<Map<String, String>>(emptyMap())
    val prayerTexts: StateFlow<Map<String, String>> = _prayerTexts.asStateFlow()

    private val _prayerTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val prayerTitles: StateFlow<Map<String, String>> = _prayerTitles.asStateFlow()

    private var currentRosaryOrder: RosaryOrder = RosaryOrder.DOMINICAN

    private fun physicalBeadForDominican(stepIndex: Int): Int {
        val lastStep = _beads.value.lastIndex
        return when {
            stepIndex == 0         -> -1
            stepIndex in 1..4      -> stepIndex - 1
            stepIndex == 5         -> 4
            stepIndex == lastStep  -> 59
            else -> {
                val loopStep = stepIndex - 6
                val decade   = loopStep / 14
                val within   = loopStep % 14
                val start    = 4 + decade * 11
                when {
                    within <= 1     -> start
                    within in 2..11 -> start + (within - 1)
                    else            -> start + 11
                }
            }
        }
    }

    private fun physicalBeadForFatima(stepIndex: Int): Int {
        val lastStep = _beads.value.lastIndex
        return when {
            stepIndex == 0                    -> -1
            stepIndex == 1                    -> 59
            stepIndex == lastStep             -> 0
            stepIndex > lastStep - 4          -> lastStep - stepIndex
            else -> {
                val loopStep = stepIndex - 2
                val decade   = loopStep / 15
                val within   = loopStep % 15
                when {
                    within <= 1     -> if (decade == 0) 59 else 59 - 11 * decade
                    within in 2..11 -> (60 - 11 * decade) - within
                    else            -> 59 - 11 * (decade + 1)
                }
            }
        }
    }

    private fun physicalBeadFor(stepIndex: Int): Int = when (currentRosaryOrder) {
        RosaryOrder.FATIMA    -> physicalBeadForFatima(stepIndex)
        RosaryOrder.DOMINICAN -> physicalBeadForDominican(stepIndex)
    }

    val currentPhysicalBead: StateFlow<Int> = _currentPosition
        .map { physicalBeadFor(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    val visitedPhysBeads: StateFlow<Set<Int>> = _currentPosition
        .map { pos -> (0..pos).mapNotNull { physicalBeadFor(it).takeIf { it >= 0 } }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val rosaryOrder: StateFlow<RosaryOrder> = prefsRepository.preferences
        .map { it.rosaryOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RosaryOrder.DOMINICAN)

    val hapticFeedback: StateFlow<Boolean> = prefsRepository.preferences
        .map { it.rosaryHapticFeedback }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val intentionInDesign: StateFlow<Boolean> = prefsRepository.preferences
        .map { it.rosaryIntentionInDesign }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val intentions: StateFlow<List<String>> = prefsRepository.preferences
        .map { it.rosaryIntentions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), List(5) { "" })

    val currentMysteryNumber: StateFlow<Int?> = _currentPosition
        .map { pos ->
            val beads = _beads.value
            (pos downTo 0).firstOrNull { i ->
                val b = beads.getOrNull(i)
                b?.prayerId == null && b?.mysteryTitle != null && b?.mysteryNumber != null
            }?.let { i -> beads[i].mysteryNumber }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveIntentions(intentions: List<String>) {
        viewModelScope.launch { prefsRepository.setRosaryIntentions(intentions) }
    }

    fun saveIntentionForMystery(index: Int, text: String) {
        val current = intentions.value.toMutableList()
        while (current.size < 5) current.add("")
        current[index] = text
        viewModelScope.launch { prefsRepository.setRosaryIntentions(current) }
    }

    fun clearIntentions() {
        viewModelScope.launch { prefsRepository.clearRosaryIntentions() }
    }

    private val _sessionId = MutableStateFlow<String?>(null)
    private var currentLanguage = "en"

    private val _completing = MutableStateFlow(false)
    val completing: StateFlow<Boolean> = _completing.asStateFlow()

    /** Current rite — used by screen to determine which language to show for mystery titles. */
    val currentRite: StateFlow<AppRite> = prefsRepository.preferences
        .map { it.appRite }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppRite.MODERN)

    // Eagerly (not WhileSubscribed) — this drives narration auto-advance logic in
    // startSession()/navigateTo(), not just UI display, so it must not depend on a
    // Composable having subscribed first.
    val narrationEnabled: StateFlow<Boolean> = prefsRepository.preferences
        .map { it.rosaryNarrationEnabled }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    /** Bumped on every manual navigation or narration toggle to invalidate stale TTS callbacks. */
    private var narrationGeneration = 0

    init {
        // React to rite or language changes — MODERN rite uses the user's appLanguage
        viewModelScope.launch {
            prefsRepository.preferences
                .distinctUntilChangedBy { it.appRite to it.appLanguage }
                .collectLatest { prefs ->
                    val mysteryLang = if (prefs.appRite == AppRite.MODERN) prefs.appLanguage else prefs.appRite.language
                    currentLanguage = if (prefs.appRite == AppRite.MODERN) prefs.appLanguage else prefs.appRite.contentLanguage
                    _mysteries.value = repository.getMysteries(mysteryLang)
                    // Traditional mode: mysteries in English, prayers in Latin
                    if (prefs.appRite == AppRite.TRADITIONAL) {
                        _englishMysteries.value = repository.getMysteries("en")
                    } else {
                        _englishMysteries.value = emptyList()
                    }
                    val prayers = prayerRepository.getAll(currentLanguage)
                    _prayerTexts.value = prayers.associate { it.id to it.body }
                    _prayerTitles.value = prayers.associate { it.id to it.title }
                }
        }
        viewModelScope.launch {
            prefsRepository.preferences
                .distinctUntilChangedBy { it.rosaryOrder }
                .collect { prefs -> currentRosaryOrder = prefs.rosaryOrder }
        }
        viewModelScope.launch {
            ttsController.events.collect { event ->
                val expectedKey = _beads.value.getOrNull(_currentPosition.value)?.let { narrationKeyFor(it) }
                when (event) {
                    is TtsEvent.SequenceCompleted -> {
                        if (event.key != expectedKey) return@collect
                        _isSpeaking.value = false
                        if (narrationEnabled.value && _currentPosition.value < _beads.value.lastIndex) {
                            navigateTo(_currentPosition.value + 1)
                        }
                    }
                    is TtsEvent.SequenceInterrupted -> {
                        if (event.key == expectedKey) _isSpeaking.value = false
                    }
                }
            }
        }
    }

    fun startSession(mysteryId: String) {
        if (_beads.value.isNotEmpty()) return
        viewModelScope.launch {
            val prefs = prefsRepository.preferences.first()
            val variant = prefs.rosaryOrder.name.lowercase()
            val beads = repository.getBeads(mysteryId, currentLanguage, variant)
            if (beads.isNotEmpty()) {
                _beads.value = beads
            } else {
                currentRosaryOrder = RosaryOrder.DOMINICAN
                _beads.value = repository.getBeads(mysteryId, currentLanguage, "dominican")
            }
            // In Traditional mode, also load English beads for scripture/meditation
            if (prefs.appRite == AppRite.TRADITIONAL) {
                val englishVariant = prefs.rosaryOrder.name.lowercase()
                val enBeads = repository.getBeads(mysteryId, "en", englishVariant)
                _englishBeads.value = _englishBeads.value.toMutableMap().apply {
                    put(mysteryId, enBeads)
                }
            }
            _currentPosition.value = 0
            _currentMysteryId.value = mysteryId
            _sessionId.value = repository.startSession(mysteryId)
            // Use the freshly-fetched prefs rather than the narrationEnabled StateFlow:
            // that flow is WhileSubscribed and may not have delivered its first value yet
            // this early in the session, which would wrongly skip narrating bead 0.
            if (prefs.rosaryNarrationEnabled) speakCurrentBead()
        }
    }

    fun advance() = navigateTo(_currentPosition.value + 1)

    fun back() = navigateTo(_currentPosition.value - 1)

    private fun navigateTo(target: Int) {
        if (target !in _beads.value.indices) return
        narrationGeneration++
        ttsController.stop()
        _isSpeaking.value = false
        _currentPosition.value = target
        maybeSpeakCurrentBead()
    }

    fun setNarrationEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setRosaryNarrationEnabled(enabled) }
        narrationGeneration++
        if (enabled) {
            speakCurrentBead()
        } else {
            ttsController.stop()
            _isSpeaking.value = false
        }
    }

    fun stopNarration() {
        narrationGeneration++
        ttsController.stop()
        _isSpeaking.value = false
    }

    private fun narrationKeyFor(bead: MysteryBead): String = "bead-${bead.id}-$narrationGeneration"

    private fun maybeSpeakCurrentBead() {
        if (narrationEnabled.value) speakCurrentBead()
    }

    private fun speakCurrentBead() {
        val bead = _beads.value.getOrNull(_currentPosition.value) ?: return
        val (parts, languageCode) = buildNarrationContent(bead) ?: return
        _isSpeaking.value = true
        ttsController.speak(parts, languageCode, key = narrationKeyFor(bead))
    }

    /** Builds the ordered text parts to narrate for [bead], and the language to speak them in. */
    private fun buildNarrationContent(bead: MysteryBead): Pair<List<String>, String>? {
        val isAnnouncement = bead.prayerId == null && bead.mysteryTitle != null
        return if (isAnnouncement) {
            val rite = currentRite.value
            val english = if (rite == AppRite.TRADITIONAL) {
                _englishBeads.value[_currentMysteryId.value]?.find { it.mysteryNumber == bead.mysteryNumber }
            } else null
            val title = english?.mysteryTitle ?: bead.mysteryTitle
            val scripture = english?.mysteryScripture ?: bead.mysteryScripture
            val meditation = english?.mysteryMeditation ?: bead.mysteryMeditation
            val language = if (rite == AppRite.TRADITIONAL) "en" else currentLanguage
            val parts = listOfNotNull(title, scripture, meditation)
            if (parts.isEmpty()) null else parts to language
        } else {
            val body = bead.prayerId?.let { _prayerTexts.value[it] } ?: return null
            listOf(body) to currentLanguage
        }
    }

    fun completeSession(onDone: () -> Unit) {
        if (_completing.value) return
        _completing.value = true
        viewModelScope.launch {
            _sessionId.value?.let { repository.completeSession(it) }
            onDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsController.shutdown()
    }
}
