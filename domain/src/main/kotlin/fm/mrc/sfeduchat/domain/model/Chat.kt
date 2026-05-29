package fm.mrc.sfeduchat.domain.model

data class Chat(
    val id: String,
    val participantUid: String,
    val participantUsername: String,
    val participantAvatarPath: String? = null,
    val lastMessagePreview: String? = null,
    val lastMessageTimestamp: Long = 0L,
)
