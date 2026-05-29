package fm.mrc.sfeduchat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fm.mrc.sfeduchat.domain.model.EncryptedMessage
import fm.mrc.sfeduchat.domain.repository.ChatRepository
import fm.mrc.sfeduchat.domain.repository.KeyRepository
import fm.mrc.sfeduchat.domain.repository.MessageRepository
import fm.mrc.sfeduchat.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<EncryptedMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val sessionCount: Int = 0,
    val rotationHint: String? = null,
)

class ChatViewModel(
    private val chatId: String,
    private val participantUid: String,
    private val messageRepository: MessageRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val keyRepository: KeyRepository,
    private val chatRepository: ChatRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        // Mark chat as read when opened
        viewModelScope.launch {
            chatRepository.markAsRead(chatId)
        }
        viewModelScope.launch {
            messageRepository.observeMessages(chatId).collect { list ->
                _state.update { it.copy(messages = list) }
                // Mark messages as read when they are loaded
                list.filter { !it.isOutgoing && it.deliveryStatus != fm.mrc.sfeduchat.domain.model.DeliveryStatus.READ }
                    .forEach { msg ->
                        viewModelScope.launch {
                            messageRepository.updateDeliveryStatus(msg.id, fm.mrc.sfeduchat.domain.model.DeliveryStatus.READ)
                        }
                    }
            }
        }
        viewModelScope.launch {
            messageRepository.observeRemoteUpdates(chatId).collect { }
        }
        viewModelScope.launch {
            val count = keyRepository.getSessionMessageCount(chatId)
            _state.update {
                it.copy(
                    sessionCount = count,
                    rotationHint = if (count >= 10) "Рекомендуется смена сессионного контекста" else null,
                )
            }
        }
    }

    fun onInputChange(text: String) = _state.update { it.copy(inputText = text, error = null) }

    fun sendMessage() {
        val text = _state.value.inputText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            sendMessageUseCase(chatId, participantUid, text)
                .onSuccess { _state.update { it.copy(isSending = false, inputText = "") } }
                .onFailure { e -> _state.update { it.copy(isSending = false, error = e.message) } }
        }
    }

    fun sendImage(bytes: ByteArray, mimeType: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }
            messageRepository.sendImageMessage(chatId, participantUid, bytes, mimeType)
                .onSuccess { _state.update { it.copy(isSending = false) } }
                .onFailure { e -> _state.update { it.copy(isSending = false, error = e.message) } }
        }
    }

    fun resetSession() {
        viewModelScope.launch {
            keyRepository.resetSessionKey(chatId)
            _state.update { it.copy(sessionCount = 0, rotationHint = null) }
        }
    }
}
