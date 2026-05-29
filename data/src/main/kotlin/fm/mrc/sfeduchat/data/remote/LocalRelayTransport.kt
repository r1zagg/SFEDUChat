package fm.mrc.sfeduchat.data.remote

import fm.mrc.sfeduchat.data.local.dao.RelayMessageDao
import fm.mrc.sfeduchat.data.local.dao.UserDao
import fm.mrc.sfeduchat.data.local.entity.RelayMessageEntity
import fm.mrc.sfeduchat.data.remote.dto.RemoteMessageDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
/**
 * Демо-транспорт: имитирует сервер через Room.
 * Два пользователя на одном эмуляторе видят общие relay_messages.
 */
class LocalRelayTransport(
    private val relayDao: RelayMessageDao,
    private val userDao: UserDao,
) : MessageTransport {

    override val isAvailable: Boolean = true

    override suspend fun publishPublicKey(uid: String, publicKeyBase64: String) {
        userDao.updatePublicKey(uid, publicKeyBase64)
    }

    override suspend fun fetchPublicKey(uid: String): String? =
        userDao.findByUid(uid)?.publicKeyBase64

    override suspend fun sendMessage(chatId: String, message: RemoteMessageDto) {
        relayDao.insert(
            RelayMessageEntity(
                id = message.id,
                chatId = chatId,
                senderUid = message.senderUid,
                senderUsername = message.senderUsername,
                ciphertextBase64 = message.message,
                ivBase64 = message.iv,
                encryptedAesKeyBase64 = message.encryptedAesKey,
                hmacBase64 = message.hmac,
                timestamp = message.timestamp,
                messageType = message.messageType,
                attachmentMimeType = message.attachmentMimeType,
                deliveryStatus = message.deliveryStatus,
                recipientUid = message.recipientUid,
            ),
        )
    }

    override fun observeMessages(chatId: String, recipientUid: String): Flow<List<RemoteMessageDto>> =
        relayDao.observeForRecipient(chatId, recipientUid).map { list ->
            list.map { it.toDto() }
        }

    override suspend fun updateDeliveryStatus(chatId: String, messageId: String, status: String) {
        relayDao.updateStatus(messageId, status)
    }

    private fun RelayMessageEntity.toDto() = RemoteMessageDto(
        id = id,
        senderUid = senderUid,
        senderUsername = senderUsername,
        message = ciphertextBase64,
        encryptedAesKey = encryptedAesKeyBase64,
        iv = ivBase64,
        hmac = hmacBase64,
        timestamp = timestamp,
        messageType = messageType,
        attachmentMimeType = attachmentMimeType,
        deliveryStatus = deliveryStatus,
        recipientUid = recipientUid,
    )
}
