package fm.mrc.sfeduchat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Локальный «сервер» для демо без Firebase — общая таблица для всех пользователей на устройстве. */
@Entity(tableName = "relay_messages")
data class RelayMessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderUid: String,
    val senderUsername: String,
    val ciphertextBase64: String,
    val ivBase64: String,
    val encryptedAesKeyBase64: String,
    val hmacBase64: String,
    val timestamp: Long,
    val messageType: String,
    val attachmentMimeType: String? = null,
    val deliveryStatus: String = "SENT",
    val recipientUid: String,
)
