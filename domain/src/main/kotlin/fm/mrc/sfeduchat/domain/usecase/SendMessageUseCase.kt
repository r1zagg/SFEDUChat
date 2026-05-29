package fm.mrc.sfeduchat.domain.usecase

import fm.mrc.sfeduchat.domain.repository.MessageRepository

class SendMessageUseCase(
    private val messageRepository: MessageRepository,
) {
    suspend operator fun invoke(chatId: String, recipientUid: String, text: String) =
        messageRepository.sendTextMessage(chatId, recipientUid, text)
}
