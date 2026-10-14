package be.lorenzogallo.pepsmobile.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.lorenzogallo.pepsmobile.data.model.AuthException
import be.lorenzogallo.pepsmobile.data.model.Lesson
import be.lorenzogallo.pepsmobile.data.model.SchoolYear
import be.lorenzogallo.pepsmobile.data.model.mondayOfWeek
import be.lorenzogallo.pepsmobile.data.model.toUserMessage
import be.lorenzogallo.pepsmobile.data.repository.PepsRepository
import be.lorenzogallo.pepsmobile.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AgendaState(
    val schoolYear: SchoolYear,
    val weekStart: LocalDate,
    val selectedDate: LocalDate,
    val lessons: UiState<List<Lesson>> = UiState.Loading,
    val refreshing: Boolean = false,
) {
    val weekEnd: LocalDate get() = weekStart.plusDays(6)
    val weekNumber: Int get() = SchoolYear.isoWeek(weekStart)
    val canGoPrevious: Boolean get() = weekStart.isAfter(schoolYear.firstWeekStart)
    val canGoNext: Boolean get() = weekStart.isBefore(schoolYear.lastWeekStart)
    val isCurrentWeek: Boolean get() = weekStart == LocalDate.now().mondayOfWeek()
}

class AgendaViewModel(private val repository: PepsRepository) : ViewModel() {
    private var loadJob: Job? = null

    private val _state: MutableStateFlow<AgendaState>
    val state: StateFlow<AgendaState>

    init {
        val today = LocalDate.now()
        val year = SchoolYear.forDate(today)
        val weekStart = year.clampWeek(today.mondayOfWeek())
        val selected = if (today.mondayOfWeek() == weekStart) today else weekStart
        _state = MutableStateFlow(AgendaState(schoolYear = year, weekStart = weekStart, selectedDate = selected))
        state = _state.asStateFlow()
        load(weekStart, force = false)
    }

    fun previousWeek() = goToWeek(_state.value.weekStart.minusWeeks(1))

    fun nextWeek() = goToWeek(_state.value.weekStart.plusWeeks(1))

    fun today() {
        val today = LocalDate.now()
        val monday = _state.value.schoolYear.clampWeek(today.mondayOfWeek())
        goToWeek(monday, preferred = today)
    }

    fun selectDay(date: LocalDate) = _state.update { it.copy(selectedDate = date) }

    fun showDate(date: LocalDate) {
        val monday = date.mondayOfWeek()
        val current = _state.value.schoolYear
        val target = if (current.containsWeek(monday)) current else SchoolYear.forDate(date)
        if (!target.containsWeek(monday)) return
        if (target != current) _state.update { it.copy(schoolYear = target) }
        goToWeek(monday, preferred = date)
    }

    fun refresh() = load(_state.value.weekStart, force = true)

    private fun goToWeek(monday: LocalDate, preferred: LocalDate? = null) {
        val year = _state.value.schoolYear
        if (!year.containsWeek(monday)) return
        val today = LocalDate.now()
        val selected = when {
            preferred != null && preferred.mondayOfWeek() == monday -> preferred
            today.mondayOfWeek() == monday -> today
            else -> monday
        }
        _state.update { it.copy(weekStart = monday, selectedDate = selected) }
        load(monday, force = false)
    }

    private fun load(weekStart: LocalDate, force: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update {
                if (force && it.lessons is UiState.Success) it.copy(refreshing = true)
                else it.copy(lessons = UiState.Loading, refreshing = false)
            }
            try {
                val lessons = repository.fetchAgenda(weekStart, force = force)
                _state.update { current ->
                    if (current.weekStart == weekStart) current.copy(lessons = UiState.Success(lessons), refreshing = false)
                    else current
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { current ->
                    if (current.weekStart == weekStart) {
                        current.copy(lessons = UiState.Error(e.toUserMessage(), needsLogin = e is AuthException), refreshing = false)
                    } else current
                }
            }
        }
    }
}
