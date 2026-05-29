package fm.mrc.sfeduchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fm.mrc.sfeduchat.data.local.entity.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun findByUid(uid: String): UserEntity?

    @Query("UPDATE users SET publicKeyBase64 = :publicKey WHERE uid = :uid")
    suspend fun updatePublicKey(uid: String, publicKey: String)

    @Query("UPDATE users SET avatarPath = :avatarPath WHERE uid = :uid")
    suspend fun updateAvatar(uid: String, avatarPath: String)
}
