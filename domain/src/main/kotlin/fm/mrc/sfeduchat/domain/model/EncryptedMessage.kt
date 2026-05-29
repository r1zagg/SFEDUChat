package fm.mrc.sfeduchat.domain.model

enum class DeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
}

enum class MessageType {
    TEXT,
    IMAGE,
}

/**
 * Сообщение в чате. [plainText] заполняется только после расшифровки на устройстве.
 * В БД и на «сервере» хранятся только криптографические поля.
 */
data class EncryptedMessage(
    val id: String,
    val chatId: String,
    val senderUid: String,
    val senderUsername: String,
    val senderAvatarPath: String? = null,
    val ciphertextBase64: String,
    val ivBase64: String,
    val encryptedAesKeyBase64: String,
    val hmacBase64: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.SENT,
    val messageType: MessageType = MessageType.TEXT,
    val attachmentMimeType: String? = null,
    val plainText: String? = null,
    val decryptError: String? = null,
)
