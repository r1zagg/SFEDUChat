package fm.mrc.sfeduchat.presentation.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fm.mrc.sfeduchat.data.local.AvatarStorage
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.usecase.LoginUseCase
import fm.mrc.sfeduchat.domain.usecase.RegisterUseCase
import fm.mrc.sfeduchat.ui.avatar.AvatarCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val username: String = "",
    val password: String = "",
    val selectedPresetId: String = AvatarCatalog.presets.first().id,
    val avatarUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository,
    private val avatarStorage: AvatarStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onUsernameChange(v: String) = _state.update { it.copy(username = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }

    fun onPresetSelected(presetId: String) = _state.update {
        it.copy(selectedPresetId = presetId, avatarUri = null, error = null)
    }

    fun onGalleryAvatarSelected(uri: Uri?) = _state.update {
        it.copy(avatarUri = uri, error = null)
    }

    fun login() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loginUseCase(state.value.username, state.value.password)
                .onSuccess { _state.update { it.copy(isLoading = false, success = true) } }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun register() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val presetPath = AvatarCatalog.pathForPreset(state.value.selectedPresetId)
            registerUseCase(state.value.username, state.value.password, presetPath)
                .onSuccess { user ->
                    state.value.avatarUri?.let { uri ->
                        runCatching {
                            val path = avatarStorage.saveAvatar(user.uid, uri)
                            authRepository.updateAvatar(user.uid, path)
                        }
                    }
                    _state.update { it.copy(isLoading = false, success = true) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
