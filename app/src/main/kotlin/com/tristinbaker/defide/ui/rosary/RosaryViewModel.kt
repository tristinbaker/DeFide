package com.tristinbaker.defide.ui.rosary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tristinbaker.defide.data.model.Mystery
import com.tristinbaker.defide.data.model.MysteryBead
import com.tristinbaker.defide.data.preferences.RosaryDiagramStyle
import com.tristinbaker.defide.data.preferences.RosaryOrder
import com.tristinbaker.defide.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.tristinbaker.defide.data.repository.PrayerRepository
import com.tristinbaker.defide.data.repository.RosaryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
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

    private var currentRosaryOrder: RosaryOrder = RosaryOrder.DOMINICAN

    /**
     * Dominican order — 78 steps (0..77).
     *  -1  = cross / Creed                  (step 0)
     *   0  = Our Father tail bead           (step 1)
     *  1–3 = 3 intro Hail Marys             (steps 2–4)
     *   4  = connector bead                 (step 5: last intro Glory Be)
     *   4, 15, 26, 37, 48 = Our Father beads (announcement=within 0, Our Father=within 1 share big bead)
     *  5–14, 16–25, 27–36, 38–47, 49–58 = Hail Mary beads only (within 2..11)
     *  15, 26, 37, 48, 59 = extras (Glory Be + Fatima prayer) on next big bead (within 12,13)
     *  59 = junction                         (last step: Hail Holy Queen)
     */
    private fun physicalBeadForDominican(stepIndex: Int): Int {
        val lastStep = _beads.value.lastIndex
        return when {
            stepIndex == 0        -> -1
            stepIndex in 1..4     -> stepIndex - 1
            stepIndex == 5        -> 4              // last intro Glory Be on connector bead
            stepIndex == lastStep -> 59
            else -> {
                val loopStep = stepIndex - 6        // step 6 = announcement m1 → loopStep 0
                val decade   = loopStep / 14
                val within   = loopStep % 14
                val start    = 4 + decade * 11
                when {
                    within <= 1     -> start             // announcement + Our Father on big bead
                    within in 2..11 -> start + (within - 1)  // Hail Marys 1–10 on small beads
                    else            -> start + 11        // Glory Be + extras → next Our Father bead
                }
            }
        }
    }

    /**
     * Fátima order — 81 steps (0..80).
     *  -1  = cross / opening prayer          (step 0)
     *  59  = junction                        (step 1: intro Glory Be; steps 2–3: announcement + Our Father)
     *  58→49 = Hail Marys decade 1           (counterclockwise)
     *  48, 37, 26, 15 = Our Father beads decades 2–5 (also hold extras + announcement of next decade)
     *  47→38, 36→27, 25→16, 14→5 = Hail Mary beads decades 2–5
     *   4  = connector tail bead             (decade 5 extras: Glory Be, Ó Maria, Fátima prayer)
     *  3→1 = closing 3 Hail Marys            (steps 77–79, traversing tail toward cross)
     *   0  = Our Father tail bead            (last step: Hail Holy Queen / Salve Regina)
     */
    private fun physicalBeadForFatima(stepIndex: Int): Int {
        val lastStep = _beads.value.lastIndex  // = 80
        return when {
            stepIndex == 0                    -> -1
            stepIndex == 1                    -> 59  // intro Glory Be at junction (on loop)
            stepIndex == lastStep             -> 0   // Hail Holy Queen on the far tail bead
            stepIndex > lastStep - 4          -> lastStep - stepIndex  // steps 77–79 → physBeads 3–1 (toward cross)
            else -> {
                // Counterclockwise: steps 2..76 = 75 steps = 5 decades × 15
                // Decade 0: announcement + OurFather at junction (59), HMs 58→49, extras on 48
                // Decades 1–4: announcement + OurFather at 48/37/26/15, HMs going down, extras on next big bead
                val loopStep = stepIndex - 2
                val decade   = loopStep / 15
                val within   = loopStep % 15
                when {
                    within <= 1     -> if (decade == 0) 59 else 59 - 11 * decade
                    within in 2..11 -> (60 - 11 * decade) - within
                    else            -> 59 - 11 * (decade + 1)  // extras → next Our Father bead (48/37/26/15/4)
                }
            }
        }
    }

    private fun physicalBeadFor(stepIndex: Int): Int = when (currentRosaryOrder) {
        RosaryOrder.FATIMA    -> physicalBeadForFatima(stepIndex)
        RosaryOrder.DOMINICAN -> physicalBeadForDominican(stepIndex)
    }

    /** Physical bead index (−1 = cross, 0–59 = beads). Drives the diagram and counter. */
    val currentPhysicalBead: StateFlow<Int> = _currentPosition
        .map { physicalBeadFor(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1)

    /** Set of physBead indices (0–59) visited so far. Used for diagram colouring. */
    val visitedPhysBeads: StateFlow<Set<Int>> = _currentPosition
        .map { pos -> (0..pos).mapNotNull { physicalBeadFor(it).takeIf { it >= 0 } }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val rosaryOrder: StateFlow<RosaryOrder> = prefsRepository.preferences
        .map { it.rosaryOrder }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RosaryOrder.DOMINICAN)

    val diagramStyle: StateFlow<RosaryDiagramStyle> = prefsRepository.preferences
        .map { it.rosaryDiagramStyle }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RosaryDiagramStyle.CLASSIC)

    fun setDiagramStyle(style: RosaryDiagramStyle) {
        viewModelScope.launch { prefsRepository.setRosaryDiagramStyle(style) }
    }

    private val _sessionId = MutableStateFlow<String?>(null)
    private var currentLanguage = "en"

    init {
        viewModelScope.launch {
            prefsRepository.preferences
                .distinctUntilChangedBy { it.appLanguage }
                .collectLatest { prefs ->
                    currentLanguage = prefs.appLanguage
                    _mysteries.value = repository.getMysteries(currentLanguage)
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
    }

    fun startSession(mysteryId: String) {
        if (_beads.value.isNotEmpty()) return  // Already started — preserve position
        viewModelScope.launch {
            val prefs = prefsRepository.preferences.first()
            val variant = prefs.rosaryOrder.name.lowercase()
            val beads = repository.getBeads(mysteryId, prefs.appLanguage, variant)
            if (beads.isNotEmpty()) {
                _beads.value = beads
            } else {
                // Language doesn't support this order variant — fall back to Dominican
                currentRosaryOrder = RosaryOrder.DOMINICAN
                _beads.value = repository.getBeads(mysteryId, prefs.appLanguage, "dominican")
            }
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
