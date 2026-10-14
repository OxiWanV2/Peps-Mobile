package be.lorenzogallo.pepsmobile.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.lorenzogallo.pepsmobile.data.model.toUserMessage
import be.lorenzogallo.pepsmobile.data.repository.PepsRepository
import be.lorenzogallo.pepsmobile.di.SessionScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = !loading && email.contains('@') && password.isNotEmpty()
}

class AuthViewModel(
    private val repository: PepsRepository,
    private val sessionScope: SessionScope,
    private val onSessionChanged: () -> Unit = {},
) : ViewModel() {
    private val _ui = MutableStateFlow(LoginUiState(email = repository.storedEmail().orEmpty()))
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    private val _loggedIn = MutableStateFlow(repository.hasStoredCredentials())
    val loggedIn: StateFlow<Boolean> = _loggedIn.asStateFlow()

    private val _loggingOut = MutableStateFlow(false)
    val loggingOut: StateFlow<Boolean> = _loggingOut.asStateFlow()

    fun onEmailChange(value: String) = _ui.update { it.copy(email = value.trim(), error = null) }

    fun onPasswordChange(value: String) = _ui.update { it.copy(password = value, error = null) }

    fun login() {
        val state = _ui.value
        if (!state.canSubmit) return
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            try {
                repository.login(state.email, state.password)
                _ui.update { it.copy(loading = false, password = "") }
                _loggedIn.value = true
                onSessionChanged()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(loading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _loggingOut.value = true
            try {
                repository.logout()
            } finally {
                _loggingOut.value = false
                _ui.value = LoginUiState()
                _loggedIn.value = false
                sessionScope.reset()
                onSessionChanged()
            }
        }
    }

    fun storedEmail(): String? = repository.storedEmail()
}
