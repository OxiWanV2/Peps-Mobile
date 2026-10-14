package be.lorenzogallo.pepsmobile.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.lorenzogallo.pepsmobile.data.model.AuthException
import be.lorenzogallo.pepsmobile.data.model.CourseTab
import be.lorenzogallo.pepsmobile.data.model.PepsPage
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

data class CourseTabState(
    val page: UiState<PepsPage> = UiState.Loading,
    val periodPath: String? = null,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
)

data class CourseState(
    val courseId: Int,
    val tab: CourseTab,
    val tabs: Map<CourseTab, CourseTabState> = CourseTab.entries.associateWith { CourseTabState() },
) {
    val current: CourseTabState get() = tabs.getValue(tab)
    val title: String? get() = tabs.values.firstNotNullOfOrNull { (it.page as? UiState.Success)?.data?.title }
}

class CourseViewModel(
    private val repository: PepsRepository,
    courseId: Int,
    initialTab: CourseTab,
) : ViewModel() {
    private val _state = MutableStateFlow(CourseState(courseId = courseId, tab = initialTab))
    val state: StateFlow<CourseState> = _state.asStateFlow()
    private val jobs = HashMap<CourseTab, Job>()

    init {
        load(initialTab, initial = true)
    }

    fun selectTab(tab: CourseTab) {
        if (tab == _state.value.tab) return
        _state.update { it.copy(tab = tab) }
        if (!_state.value.tabs.getValue(tab).loaded) load(tab, initial = true)
    }

    fun selectPeriod(path: String) {
        val tab = _state.value.tab
        if (path == _state.value.tabs.getValue(tab).periodPath) return
        updateTab(tab) { it.copy(periodPath = path) }
        load(tab, initial = true)
    }

    fun refresh() = load(_state.value.tab, initial = false)

    private fun load(tab: CourseTab, initial: Boolean) {
        jobs[tab]?.cancel()
        jobs[tab] = viewModelScope.launch {
            updateTab(tab) { if (initial) it.copy(page = UiState.Loading) else it.copy(refreshing = true) }
            try {
                val periodPath = _state.value.tabs.getValue(tab).periodPath
                val page = repository.fetchCoursePage(_state.value.courseId, tab, periodPath, force = !initial)
                updateTab(tab) { it.copy(page = UiState.Success(page), refreshing = false, loaded = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateTab(tab) {
                    it.copy(page = UiState.Error(e.toUserMessage(), needsLogin = e is AuthException), refreshing = false)
                }
            }
        }
    }

    private fun updateTab(tab: CourseTab, transform: (CourseTabState) -> CourseTabState) {
        _state.update { s -> s.copy(tabs = s.tabs + (tab to transform(s.tabs.getValue(tab)))) }
    }
}
