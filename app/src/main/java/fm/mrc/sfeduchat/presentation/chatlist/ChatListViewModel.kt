package fm.mrc.sfeduchat.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fm.mrc.sfeduchat.domain.model.Chat
import fm.mrc.sfeduchat.domain.model.User
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatListUiState(
    val chats: List<Chat> = emptyList(),
    val currentUser: User? = null,
    val newChatUsername: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdChat: Chat? = null,
)

class ChatListViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    val currentUser = authRepository.currentUser.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    init {
        viewModelScope.launch {
            chatRepository.observeChats().collect { chats ->
                _uiState.update { it.copy(chats = chats) }
            }
        }
        viewModelScope.launch {
            currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun onNewChatUsernameChange(v: String) = _uiState.update { it.copy(newChatUsername = v) }

    fun createChat() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            chatRepository.createChatWithUsername(_uiState.value.newChatUsername)
                .onSuccess { chat ->
                    _uiState.update {
                        it.copy(isLoading = false, createdChat = chat, newChatUsername = "")
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun clearCreatedChat() = _uiState.update { it.copy(createdChat = null) }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
