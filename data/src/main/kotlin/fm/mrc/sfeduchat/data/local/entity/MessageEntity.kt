package fm.mrc.sfeduchat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderUid: String,
    val senderUsername: String,
    val senderAvatarPath: String? = null,
    val ciphertextBase64: String,
    val ivBase64: String,
    val encryptedAesKeyBase64: String,
    val encryptedAesKeySelfBase64: String = "",
    val hmacBase64: String,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val deliveryStatus: String,
    val messageType: String,
    val attachmentMimeType: String? = null,
)
