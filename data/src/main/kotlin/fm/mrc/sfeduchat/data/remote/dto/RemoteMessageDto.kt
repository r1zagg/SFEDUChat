package fm.mrc.sfeduchat.data.remote.dto

/** Структура сообщения в Firebase Realtime Database. */
data class RemoteMessageDto(
    val id: String = "",
    val senderUid: String = "",
    val senderUsername: String = "",
    val message: String = "",
    val encryptedAesKey: String = "",
    val iv: String = "",
    val hmac: String = "",
    val timestamp: Long = 0L,
    val messageType: String = "TEXT",
    val attachmentMimeType: String? = null,
    val deliveryStatus: String = "SENT",
    val recipientUid: String = "",
)
