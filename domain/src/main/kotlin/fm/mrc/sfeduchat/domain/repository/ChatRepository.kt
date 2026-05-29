package fm.mrc.sfeduchat.domain.repository

import fm.mrc.sfeduchat.domain.model.Chat
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChats(): Flow<List<Chat>>
    suspend fun createChatWithUsername(username: String): Result<Chat>
    suspend fun findUserByUsername(username: String): Result<String>
    suspend fun markAsRead(chatId: String)
}
