package be.lorenzogallo.pepsmobile.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.lorenzogallo.pepsmobile.data.model.AuthException
import be.lorenzogallo.pepsmobile.data.model.PepsPage
import be.lorenzogallo.pepsmobile.data.model.PeriodTab
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

data class ResultsState(
    val page: UiState<PepsPage> = UiState.Loading,
    val hubTabs: List<PeriodTab> = emptyList(),
    val hubPath: String? = null,
    val periodPath: String? = null,
    val refreshing: Boolean = false,
) {
    val currentPath: String get() = periodPath ?: hubPath ?: DEFAULT_PATH

    companion object {
        const val DEFAULT_PATH = "/?page=resultats"
    }
}

class ResultsViewModel(private val repository: PepsRepository) : ViewModel() {
    private val _state = MutableStateFlow(ResultsState())
    val state: StateFlow<ResultsState> = _state.asStateFlow()
    private var job: Job? = null

    init {
        load(initial = true)
    }

    fun refresh() = load(initial = false)

    fun selectHubTab(tab: PeriodTab) {
        if (tab.path == _state.value.hubPath) return
        _state.update { it.copy(hubPath = tab.path, periodPath = null) }
        load(initial = true)
    }

    fun selectPeriod(path: String) {
        if (path == _state.value.periodPath) return
        _state.update { it.copy(periodPath = path) }
        load(initial = true)
    }

    private fun load(initial: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            _state.update { if (initial) it.copy(page = UiState.Loading) else it.copy(refreshing = true) }
            try {
                val page = repository.fetchResults(_state.value.currentPath, force = !initial)
                _state.update { s ->
                    s.copy(
                        page = UiState.Success(page),
                        hubTabs = if (page.hubTabs.size > 1) page.hubTabs else s.hubTabs,
                        hubPath = s.hubPath ?: page.hubTabs.firstOrNull { it.active }?.path,
                        refreshing = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(page = UiState.Error(e.toUserMessage(), needsLogin = e is AuthException), refreshing = false) }
            }
        }
    }
}
