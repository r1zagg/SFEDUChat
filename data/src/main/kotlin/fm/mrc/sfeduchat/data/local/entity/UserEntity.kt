package fm.mrc.sfeduchat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val username: String,
    val passwordHash: String,
    val publicKeyBase64: String? = null,
    val avatarPath: String? = null,
)
