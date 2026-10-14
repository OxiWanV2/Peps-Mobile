package be.lorenzogallo.pepsmobile.ui.common

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val needsLogin: Boolean = false) : UiState<Nothing>
}
