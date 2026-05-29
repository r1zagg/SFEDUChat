package fm.mrc.sfeduchat.data.di

import android.content.Context
import fm.mrc.sfeduchat.data.local.ChatDatabase
import fm.mrc.sfeduchat.data.local.ChatDatabaseFactory
import fm.mrc.sfeduchat.data.local.dao.ChatDao
import fm.mrc.sfeduchat.data.local.dao.MessageDao
import fm.mrc.sfeduchat.data.local.dao.RelayMessageDao
import fm.mrc.sfeduchat.data.local.dao.UserDao

/** Фабрика DAO — скрывает Room от модуля app. */
class DataProviders(context: Context) {
    private val database: ChatDatabase = ChatDatabaseFactory.create(context)
    val userDao: UserDao get() = database.userDao()
    val chatDao: ChatDao get() = database.chatDao()
    val messageDao: MessageDao get() = database.messageDao()
    val relayMessageDao: RelayMessageDao get() = database.relayMessageDao()
}
