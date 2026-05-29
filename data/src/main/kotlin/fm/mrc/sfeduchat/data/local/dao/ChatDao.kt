package fm.mrc.sfeduchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fm.mrc.sfeduchat.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE ownerUid = :ownerUid ORDER BY lastMessageTimestamp DESC")
    fun observeByOwner(ownerUid: String): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(chat: ChatEntity)

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getById(chatId: String): ChatEntity?

    @Query(
        "SELECT * FROM chats WHERE ownerUid = :ownerUid AND participantUid = :participantUid LIMIT 1",
    )
    suspend fun findExisting(ownerUid: String, participantUid: String): ChatEntity?

    @Query(
        "UPDATE chats SET lastMessagePreview = :preview, lastMessageTimestamp = :ts WHERE id = :chatId AND ownerUid = :ownerUid",
    )
    suspend fun updatePreview(chatId: String, ownerUid: String, preview: String, ts: Long)

    @Query(
        "UPDATE chats SET lastMessagePreview = :preview, lastMessageTimestamp = :ts WHERE id = :chatId",
    )
    suspend fun updatePreviewAll(chatId: String, preview: String, ts: Long)
}
