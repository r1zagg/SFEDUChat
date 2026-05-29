package fm.mrc.sfeduchat.domain.repository

import fm.mrc.sfeduchat.domain.model.DeliveryStatus
import fm.mrc.sfeduchat.domain.model.EncryptedMessage
import fm.mrc.sfeduchat.domain.model.MessageType
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun observeMessages(chatId: String): Flow<List<EncryptedMessage>>
    suspend fun sendTextMessage(chatId: String, recipientUid: String, text: String): Result<Unit>
    suspend fun sendImageMessage(
        chatId: String,
        recipientUid: String,
        imageBytes: ByteArray,
        mimeType: String,
    ): Result<Unit>
    suspend fun syncRemoteMessages(chatId: String): Result<Unit>
    fun observeRemoteUpdates(chatId: String): Flow<Unit>
    suspend fun markAsRead(chatId: String, messageId: String): Result<Unit>
    suspend fun updateDeliveryStatus(messageId: String, status: DeliveryStatus): Result<Unit>
}
