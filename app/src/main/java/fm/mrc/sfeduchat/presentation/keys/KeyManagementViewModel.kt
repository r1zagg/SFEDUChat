package fm.mrc.sfeduchat.presentation.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fm.mrc.sfeduchat.domain.repository.KeyRepository
import fm.mrc.sfeduchat.domain.usecase.RegenerateKeysUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KeyUiState(
    val publicKey: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class KeyManagementViewModel(
    private val keyRepository: KeyRepository,
    private val regenerateKeysUseCase: RegenerateKeysUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(KeyUiState())
    val state: StateFlow<KeyUiState> = _state.asStateFlow()

    init {
        loadKey()
    }

    fun loadKey() {
        viewModelScope.launch {
            _state.update { it.copy(publicKey = keyRepository.getPublicKeyBase64()) }
        }
    }

    fun regenerate() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, message = null) }
            regenerateKeysUseCase()
                .onSuccess { key ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            publicKey = key,
                            message = "Новая RSA-2048 пара создана в Android Keystore",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
