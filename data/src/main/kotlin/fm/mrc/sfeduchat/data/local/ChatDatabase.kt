package fm.mrc.sfeduchat.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import fm.mrc.sfeduchat.data.local.dao.ChatDao
import fm.mrc.sfeduchat.data.local.dao.MessageDao
import fm.mrc.sfeduchat.data.local.dao.RelayMessageDao
import fm.mrc.sfeduchat.data.local.dao.UserDao
import fm.mrc.sfeduchat.data.local.entity.ChatEntity
import fm.mrc.sfeduchat.data.local.entity.MessageEntity
import fm.mrc.sfeduchat.data.local.entity.RelayMessageEntity
import fm.mrc.sfeduchat.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        RelayMessageEntity::class,
    ],
    version = 9,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun relayMessageDao(): RelayMessageDao
}
