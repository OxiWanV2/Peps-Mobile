package be.lorenzogallo.pepsmobile.ui.settings

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.lorenzogallo.pepsmobile.data.model.UserProfile
import be.lorenzogallo.pepsmobile.data.repository.PepsRepository
import be.lorenzogallo.pepsmobile.ui.common.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileState(
    val profile: UserProfile? = null,
    val avatar: Bitmap? = null,
    val loading: Boolean = true,
)

class ProfileViewModel(private val repository: PepsRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState(profile = repository.storedProfile()))
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.fetchProfile() }.getOrNull()?.let { profile -> _state.update { it.copy(profile = profile) } }
            val bitmap = runCatching { repository.fetchAvatar() }.getOrNull()?.let { bytes ->
                withContext(Dispatchers.Default) { ImageUtils.decodeScaled(bytes) }
            }
            _state.update { it.copy(avatar = bitmap, loading = false) }
        }
    }
}
