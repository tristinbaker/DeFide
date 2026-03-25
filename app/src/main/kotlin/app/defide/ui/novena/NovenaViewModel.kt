package app.defide.ui.novena

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.defide.data.db.user.entity.NovenaProgressEntity
import app.defide.data.model.Novena
import app.defide.data.model.NovenaDay
import app.defide.data.repository.NovenaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NovenaViewModel @Inject constructor(
    private val repository: NovenaRepository,
) : ViewModel() {

    private val _novenas = MutableStateFlow<List<Novena>>(emptyList())
    val novenas: StateFlow<List<Novena>> = _novenas.asStateFlow()

    // Maps novenaId -> title for display in progress screen
    private val _novenaTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val novenaTitles: StateFlow<Map<String, String>> = _novenaTitles.asStateFlow()

    private val _detail = MutableStateFlow<Novena?>(null)
    val detail: StateFlow<Novena?> = _detail.asStateFlow()

    private val _currentDay = MutableStateFlow<NovenaDay?>(null)
    val currentDay: StateFlow<NovenaDay?> = _currentDay.asStateFlow()

    private val _progress = MutableStateFlow<NovenaProgressEntity?>(null)
    val progress: StateFlow<NovenaProgressEntity?> = _progress.asStateFlow()

    val activeNovenas: StateFlow<List<NovenaProgressEntity>> = repository
        .getActiveProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedNovenas: StateFlow<List<NovenaProgressEntity>> = repository
        .getCompletedProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val all = repository.getAll()
            _novenas.value = all
            _novenaTitles.value = all.associate { it.id to it.title }
        }
    }

    fun loadDetail(novenaId: String) {
        viewModelScope.launch { _detail.value = repository.getById(novenaId) }
    }

    fun loadProgress(novenaId: String) {
        viewModelScope.launch { _progress.value = repository.getProgressForNovena(novenaId) }
    }

    fun loadCurrentDay(novenaId: String, progressId: String) {
        viewModelScope.launch {
            val prog = repository.getProgressForNovena(novenaId) ?: return@launch
            _progress.value = prog
            val nextDay = prog.lastCompletedDay + 1
            _currentDay.value = repository.getDay(novenaId, nextDay)
        }
    }

    fun startNovena(novenaId: String, startDate: String = LocalDate.now().toString(), onStarted: (String) -> Unit = {}) {
        viewModelScope.launch {
            val progressId = repository.startNovena(novenaId, startDate)
            loadProgress(novenaId)
            onStarted(progressId)
        }
    }

    fun abandonNovena(progressId: String) {
        viewModelScope.launch { repository.abandonNovena(progressId) }
    }

    fun completeDay(novenaId: String) {
        viewModelScope.launch {
            val prog = _progress.value ?: return@launch
            val nextDay = prog.lastCompletedDay + 1
            repository.completeDay(prog.id, nextDay)
            if (nextDay >= (_detail.value?.totalDays ?: 9)) {
                repository.completeNovena(prog.id)
            }
            loadCurrentDay(novenaId, prog.id)
        }
    }
}
