package fm.mrc.sfeduchat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats", primaryKeys = ["id", "ownerUid"])
data class ChatEntity(
    val id: String,
    val ownerUid: String,
    val participantUid: String,
    val participantUsername: String,
    val participantAvatarPath: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long = 0L,
)
