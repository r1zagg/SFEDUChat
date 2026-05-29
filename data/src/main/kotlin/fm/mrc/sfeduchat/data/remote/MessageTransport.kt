package fm.mrc.sfeduchat.data.remote

import fm.mrc.sfeduchat.data.remote.dto.RemoteMessageDto
import kotlinx.coroutines.flow.Flow

interface MessageTransport {
    val isAvailable: Boolean
    suspend fun publishPublicKey(uid: String, publicKeyBase64: String)
    suspend fun fetchPublicKey(uid: String): String?
    suspend fun sendMessage(chatId: String, message: RemoteMessageDto)
    fun observeMessages(chatId: String, recipientUid: String): Flow<List<RemoteMessageDto>>
    suspend fun updateDeliveryStatus(chatId: String, messageId: String, status: String)
}
