package fm.mrc.sfeduchat.domain.model

data class User(
    val uid: String,
    val username: String,
    val publicKeyBase64: String? = null,
    val avatarPath: String? = null,
)
