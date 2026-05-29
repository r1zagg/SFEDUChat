package fm.mrc.sfeduchat.data.repository

import fm.mrc.sfeduchat.data.local.dao.ChatDao
import fm.mrc.sfeduchat.data.local.dao.UserDao
import fm.mrc.sfeduchat.data.local.entity.ChatEntity
import fm.mrc.sfeduchat.data.mapper.toDomain
import fm.mrc.sfeduchat.domain.model.Chat
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val keyRepository: fm.mrc.sfeduchat.domain.repository.KeyRepository,
) : ChatRepository {

    override fun observeChats(): Flow<List<Chat>> =
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user ->
            val uid = user?.uid.orEmpty()
            println("DEBUG observeChats: uid=$uid, user=${user?.username}")
            chatDao.observeByOwner(uid).map { list ->
                println("DEBUG observeChats: uid=$uid, chats count=${list.size}")
                list.forEach { println("DEBUG observeChats: chat id=${it.id}, owner=${it.ownerUid}, participant=${it.participantUid}") }
                list.map { it.toDomain() }
            }
        }

    override suspend fun createChatWithUsername(username: String): Result<Chat> = runCatching {
        val me = authRepository.getCurrentUser() ?: error("Войдите в аккаунт")
        val peer = userDao.findByUsername(username.trim())
            ?: error("Пользователь не найден")
        if (peer.uid == me.uid) error("Нельзя создать чат с самим собой")

        // Ensure key pairs exist for both users
        keyRepository.ensureKeyPairExists()
        
        // Exchange public keys between users
        keyRepository.publishPublicKey(me.uid)
        keyRepository.publishPublicKey(peer.uid)

        val chatId = chatIdFor(me.uid, peer.uid)
        val existing = chatDao.findExisting(me.uid, peer.uid)
        if (existing != null) return@runCatching existing.toDomain()

        val entity = ChatEntity(
            id = chatId,
            ownerUid = me.uid,
            participantUid = peer.uid,
            participantUsername = peer.username,
            participantAvatarPath = peer.avatarPath,
        )
        chatDao.insert(entity)

        val reverse = ChatEntity(
            id = chatId,
            ownerUid = peer.uid,
            participantUid = me.uid,
            participantUsername = me.username,
            participantAvatarPath = me.avatarPath,
        )
        chatDao.insert(reverse)

        entity.toDomain()
    }

    override suspend fun findUserByUsername(username: String): Result<String> = runCatching {
        userDao.findByUsername(username.trim())?.uid ?: error("Не найден")
    }

    override suspend fun markAsRead(chatId: String) {
        // No-op since unread count feature is removed
    }

    companion object {
        fun chatIdFor(uid1: String, uid2: String): String =
            listOf(uid1, uid2).sorted().joinToString("_")
    }
}
