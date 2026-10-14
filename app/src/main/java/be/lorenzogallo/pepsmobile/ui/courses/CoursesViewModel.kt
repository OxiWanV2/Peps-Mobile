package be.lorenzogallo.pepsmobile.ui.courses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.lorenzogallo.pepsmobile.data.model.AuthException
import be.lorenzogallo.pepsmobile.data.model.Course
import be.lorenzogallo.pepsmobile.data.model.toUserMessage
import be.lorenzogallo.pepsmobile.data.repository.PepsRepository
import be.lorenzogallo.pepsmobile.ui.common.UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CoursesViewModel(private val repository: PepsRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Course>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Course>>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        load(force = false)
    }

    fun refresh() = load(force = true)

    private fun load(force: Boolean) {
        viewModelScope.launch {
            if (force) _refreshing.value = true else _state.value = UiState.Loading
            try {
                _state.value = UiState.Success(repository.fetchCourses(force = force))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UiState.Error(e.toUserMessage(), needsLogin = e is AuthException)
            } finally {
                _refreshing.value = false
            }
        }
    }
}
