package be.lorenzogallo.pepsmobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.lorenzogallo.pepsmobile.data.model.AgendaOverview
import be.lorenzogallo.pepsmobile.data.model.AuthException
import be.lorenzogallo.pepsmobile.data.model.NewsItem
import be.lorenzogallo.pepsmobile.data.model.PepsNotification
import be.lorenzogallo.pepsmobile.data.model.PepsPage
import be.lorenzogallo.pepsmobile.data.model.UserProfile
import be.lorenzogallo.pepsmobile.data.model.toUserMessage
import be.lorenzogallo.pepsmobile.data.prefs.AppPrefs
import be.lorenzogallo.pepsmobile.data.prefs.DashboardWidget
import be.lorenzogallo.pepsmobile.data.repository.PepsRepository
import be.lorenzogallo.pepsmobile.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

data class DashboardState(
    val widgets: List<DashboardWidget> = emptyList(),
    val profile: UserProfile? = null,
    val news: UiState<List<NewsItem>>? = null,
    val agenda: UiState<AgendaOverview>? = null,
    val results: UiState<PepsPage>? = null,
    val notifications: List<PepsNotification> = emptyList(),
    val notificationsError: String? = null,
    val refreshing: Boolean = false,
) {
    val unreadCount: Int get() = notifications.count { !it.read }
    val initialLoading: Boolean
        get() {
            val parts = listOfNotNull(news, agenda, results)
            return parts.isNotEmpty() && parts.all { it is UiState.Loading }
        }
    val needsLogin: UiState.Error?
        get() = listOfNotNull(news, agenda, results).filterIsInstance<UiState.Error>().firstOrNull { it.needsLogin }
}

class DashboardViewModel(
    private val repository: PepsRepository,
    private val prefs: AppPrefs,
    private val onDataLoaded: () -> Unit,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardState(profile = repository.storedProfile()))
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private var newsJob: Job? = null
    private var agendaJob: Job? = null
    private var resultsJob: Job? = null
    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            prefs.dashboardWidgets.collect { config ->
                _state.update { it.copy(widgets = config.filter { c -> c.enabled }.map { c -> c.widget }) }
                ensureData(force = false)
            }
        }
        loadNotifications()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            val jobs = buildList {
                val widgets = _state.value.widgets
                if (DashboardWidget.NEWS in widgets) add(loadNews(force = true))
                if (widgets.any { it in AGENDA_WIDGETS }) add(loadAgenda(force = true))
                if (DashboardWidget.RESULTS in widgets) add(loadResults(force = true))
                add(loadNotifications())
            }
            jobs.joinAll()
            _state.update { it.copy(refreshing = false) }
            onDataLoaded()
        }
    }

    private fun ensureData(force: Boolean) {
        val s = _state.value
        val widgets = s.widgets
        if (DashboardWidget.NEWS in widgets && (force || s.news == null)) loadNews(force)
        if (widgets.any { it in AGENDA_WIDGETS } && (force || s.agenda == null)) loadAgenda(force)
        if (DashboardWidget.RESULTS in widgets && (force || s.results == null)) loadResults(force)
        if (s.profile == null && DashboardWidget.TODAY in widgets) loadProfile()
    }

    private fun loadNews(force: Boolean): Job {
        newsJob?.cancel()
        return viewModelScope.launch {
            if (!force) _state.update { it.copy(news = UiState.Loading) }
            val result = runCatching { repository.fetchNews(force) }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            _state.update { it.copy(news = result.toUiState(), profile = repository.storedProfile() ?: it.profile) }
        }.also { newsJob = it }
    }

    private fun loadAgenda(force: Boolean): Job {
        agendaJob?.cancel()
        return viewModelScope.launch {
            if (!force) _state.update { it.copy(agenda = UiState.Loading) }
            val result = runCatching { repository.fetchAgendaOverview(force = force) }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            _state.update { it.copy(agenda = result.toUiState()) }
            if (result.isSuccess && !force) onDataLoaded()
        }.also { agendaJob = it }
    }

    private fun loadResults(force: Boolean): Job {
        resultsJob?.cancel()
        return viewModelScope.launch {
            if (!force) _state.update { it.copy(results = UiState.Loading) }
            val result = runCatching { repository.fetchResults(force = force) }
            result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
            _state.update { it.copy(results = result.toUiState()) }
        }.also { resultsJob = it }
    }

    private fun loadProfile() {
        if (profileJob?.isActive == true) return
        profileJob = viewModelScope.launch {
            val profile = runCatching { repository.fetchProfile() }.getOrNull() ?: return@launch
            _state.update { it.copy(profile = profile) }
        }
    }

    private fun loadNotifications(): Job = viewModelScope.launch {
        val result = runCatching { repository.fetchNotifications() }
        result.exceptionOrNull()?.let { if (it is CancellationException) throw it }
        _state.update { current ->
            current.copy(
                notifications = result.getOrElse { current.notifications },
                notificationsError = result.exceptionOrNull()?.toUserMessage(),
            )
        }
    }

    private fun <T> Result<T>.toUiState(): UiState<T> = fold(
        onSuccess = { UiState.Success(it) },
        onFailure = { e -> UiState.Error(e.toUserMessage(), needsLogin = e is AuthException) },
    )

    fun markRead(id: Long) {
        val target = _state.value.notifications.firstOrNull { it.id == id } ?: return
        if (target.read) return
        _state.update { s -> s.copy(notifications = s.notifications.map { if (it.id == id) it.copy(read = true) else it }) }
        viewModelScope.launch { runCatching { repository.markNotificationRead(id) } }
    }

    fun markAllRead() {
        if (_state.value.notifications.none { !it.read }) return
        _state.update { s -> s.copy(notifications = s.notifications.map { it.copy(read = true) }) }
        viewModelScope.launch { runCatching { repository.markAllNotificationsRead() } }
    }

    fun delete(id: Long) {
        _state.update { s -> s.copy(notifications = s.notifications.filterNot { it.id == id }) }
        viewModelScope.launch { runCatching { repository.deleteNotification(id) } }
    }

    fun deleteAll() {
        if (_state.value.notifications.isEmpty()) return
        _state.update { it.copy(notifications = emptyList()) }
        viewModelScope.launch { runCatching { repository.deleteAllNotifications() } }
    }

    private companion object {
        val AGENDA_WIDGETS = setOf(
            DashboardWidget.TODAY,
            DashboardWidget.EVAL_WEEK,
            DashboardWidget.EVAL_UPCOMING,
            DashboardWidget.HOMEWORK,
        )
    }
}
