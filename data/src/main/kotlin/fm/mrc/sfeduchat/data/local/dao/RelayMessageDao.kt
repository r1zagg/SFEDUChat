package fm.mrc.sfeduchat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fm.mrc.sfeduchat.data.local.entity.RelayMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelayMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: RelayMessageEntity)

    @Query(
        "SELECT * FROM relay_messages WHERE chatId = :chatId AND recipientUid = :recipientUid ORDER BY timestamp ASC",
    )
    fun observeForRecipient(chatId: String, recipientUid: String): Flow<List<RelayMessageEntity>>

    @Query(
        "SELECT * FROM relay_messages WHERE chatId = :chatId AND recipientUid = :recipientUid AND timestamp > :after ORDER BY timestamp ASC",
    )
    suspend fun getNewerThan(chatId: String, recipientUid: String, after: Long): List<RelayMessageEntity>

    @Query("UPDATE relay_messages SET deliveryStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}
