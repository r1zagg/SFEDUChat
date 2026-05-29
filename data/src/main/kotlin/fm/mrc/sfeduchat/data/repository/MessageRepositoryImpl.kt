package fm.mrc.sfeduchat.data.repository

import fm.mrc.sfeduchat.data.crypto.AesEncryptionManager
import fm.mrc.sfeduchat.data.crypto.HmacSignatureManager
import fm.mrc.sfeduchat.data.crypto.KeystoreManager
import fm.mrc.sfeduchat.data.crypto.MessageCryptoService
import fm.mrc.sfeduchat.data.crypto.RsaEncryptionManager
import fm.mrc.sfeduchat.data.crypto.SessionKeyManager
import fm.mrc.sfeduchat.data.local.dao.ChatDao
import fm.mrc.sfeduchat.data.local.dao.MessageDao
import fm.mrc.sfeduchat.data.local.dao.UserDao
import fm.mrc.sfeduchat.data.local.entity.MessageEntity
import fm.mrc.sfeduchat.data.mapper.toDomain
import fm.mrc.sfeduchat.data.remote.MessageTransport
import fm.mrc.sfeduchat.data.remote.dto.RemoteMessageDto
import fm.mrc.sfeduchat.domain.model.CryptoException
import fm.mrc.sfeduchat.domain.model.DecryptionException
import fm.mrc.sfeduchat.domain.model.DeliveryStatus
import fm.mrc.sfeduchat.domain.model.SignatureVerificationException
import fm.mrc.sfeduchat.domain.model.EncryptedMessage
import fm.mrc.sfeduchat.domain.model.MessageType
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.repository.KeyRepository
import fm.mrc.sfeduchat.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import fm.mrc.sfeduchat.domain.model.User
import java.util.Base64
import java.util.UUID
class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val keyRepository: KeyRepository,
    private val messageCrypto: MessageCryptoService,
    private val rsaManager: RsaEncryptionManager,
    private val keystoreManager: KeystoreManager,
    private val transport: MessageTransport,
    private val sessionKeyManager: SessionKeyManager,
    private val aesManager: AesEncryptionManager,
    private val hmacManager: HmacSignatureManager,
) : MessageRepository {

    override fun observeMessages(chatId: String): Flow<List<EncryptedMessage>> =
        authRepository.currentUser
            .distinctUntilChanged { old, new -> old?.uid == new?.uid }
            .flatMapLatest { user ->
                println("DEBUG observeMessages: user=${user?.uid}, chatId=$chatId")
                messageDao.observeByChat(chatId).map { entities ->
                    println("DEBUG observeMessages: entities count=${entities.size}")
                    entities.map { decryptEntity(it, user) }
                }
            }

    override fun observeRemoteUpdates(chatId: String): Flow<Unit> =
        authRepository.currentUser.flatMapLatest { user ->
            if (user == null) {
                kotlinx.coroutines.flow.emptyFlow()
            } else {
                transport.observeMessages(chatId, user.uid).onEach { remoteList ->
                    remoteList.forEach { ingestRemote(it, chatId, user) }
                }
            }
        }.map { }

    override suspend fun sendTextMessage(
        chatId: String,
        recipientUid: String,
        text: String,
    ): Result<Unit> = sendPayload(chatId, recipientUid, text.toByteArray(Charsets.UTF_8), MessageType.TEXT, null)

    override suspend fun sendImageMessage(
        chatId: String,
        recipientUid: String,
        imageBytes: ByteArray,
        mimeType: String,
    ): Result<Unit> = sendPayload(chatId, recipientUid, imageBytes, MessageType.IMAGE, mimeType)

    private suspend fun sendPayload(
        chatId: String,
        recipientUid: String,
        bytes: ByteArray,
        type: MessageType,
        mime: String?,
    ): Result<Unit> = runCatching {
        val me = authRepository.getCurrentUser() ?: error("Не авторизован")

        // Calculate preview from actual message content
        val preview = when (type) {
            MessageType.TEXT -> String(bytes, Charsets.UTF_8)
            MessageType.IMAGE -> "Фото"
        }

        // Ensure chat exists for sender
        val recipient = userDao.findByUid(recipientUid)
        if (recipient != null) {
            val existingChat = chatDao.findExisting(me.uid, recipientUid)
            println("DEBUG sendPayload: me.uid=${me.uid}, recipientUid=$recipientUid, existingChat=$existingChat")
            if (existingChat == null) {
                val chatEntity = fm.mrc.sfeduchat.data.local.entity.ChatEntity(
                    id = chatId,
                    ownerUid = me.uid,
                    participantUid = recipientUid,
                    participantUsername = recipient.username,
                    participantAvatarPath = recipient.avatarPath,
                    lastMessagePreview = preview,
                    lastMessageTimestamp = System.currentTimeMillis(),
                )
                chatDao.insert(chatEntity)
                println("DEBUG sendPayload: Created chat for sender with chatId=$chatId")
            }
        }
        
        // For local relay, use shared AES key instead of RSA to avoid key mismatch issues
        val sharedKey = sessionKeyManager.getOrCreate(chatId)
        val encrypted = aesManager.encrypt(bytes, sharedKey)
        val hmac = hmacManager.sign(encrypted.ciphertext, hmacManager.deriveHmacKey(sharedKey.encoded))

        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val entity = MessageEntity(
            id = messageId,
            chatId = chatId,
            senderUid = me.uid,
            senderUsername = me.username,
            senderAvatarPath = me.avatarPath,
            ciphertextBase64 = Base64.getEncoder().encodeToString(encrypted.ciphertext),
            ivBase64 = Base64.getEncoder().encodeToString(encrypted.iv),
            encryptedAesKeyBase64 = "", // Not used for shared key
            encryptedAesKeySelfBase64 = "", // Not used for shared key
            hmacBase64 = hmac,
            timestamp = timestamp,
            isOutgoing = true,
            deliveryStatus = DeliveryStatus.SENDING.name,
            messageType = type.name,
            attachmentMimeType = mime,
        )
        messageDao.insert(entity)

        transport.sendMessage(
            chatId,
            RemoteMessageDto(
                id = messageId,
                senderUid = me.uid,
                senderUsername = me.username,
                message = Base64.getEncoder().encodeToString(encrypted.ciphertext),
                encryptedAesKey = "", // Not used for shared key
                iv = Base64.getEncoder().encodeToString(encrypted.iv),
                hmac = hmac,
                timestamp = timestamp,
                messageType = type.name,
                attachmentMimeType = mime,
                deliveryStatus = DeliveryStatus.SENT.name,
                recipientUid = recipientUid,
            ),
        )
        messageDao.updateStatus(messageId, DeliveryStatus.SENT.name)
        chatDao.updatePreviewAll(chatId, preview, timestamp)

        // Ensure recipient has chat entry
        val recipientUser = userDao.findByUid(recipientUid)
        println("DEBUG sendPayload: recipientUser=$recipientUser for recipientUid=$recipientUid")
        if (recipientUser != null) {
            val existingChat = chatDao.findExisting(recipientUid, me.uid)
            println("DEBUG sendPayload: recipient existingChat=$existingChat")
            if (existingChat == null) {
                val chatEntity = fm.mrc.sfeduchat.data.local.entity.ChatEntity(
                    id = chatId,
                    ownerUid = recipientUid,
                    participantUid = me.uid,
                    participantUsername = me.username,
                    participantAvatarPath = me.avatarPath,
                    lastMessagePreview = preview,
                    lastMessageTimestamp = timestamp,
                )
                chatDao.insert(chatEntity)
                println("DEBUG sendPayload: Created chat for recipient with chatId=$chatId")
            } else {
                // Update preview for existing recipient chat
                chatDao.updatePreviewAll(chatId, preview, timestamp)
                println("DEBUG sendPayload: Updated preview for existing recipient chat with chatId=$chatId")
            }
        } else {
            println("DEBUG sendPayload: recipientUser is null, cannot create chat for recipient")
        }
        sessionKeyManager.increment(chatId)
        if (sessionKeyManager.shouldRotate(chatId)) {
            sessionKeyManager.reset(chatId)
        }
    }

    override suspend fun syncRemoteMessages(chatId: String): Result<Unit> = runCatching {
        authRepository.getCurrentUser() ?: return@runCatching
    }

    override suspend fun markAsRead(chatId: String, messageId: String): Result<Unit> = runCatching {
        messageDao.updateStatus(messageId, DeliveryStatus.READ.name)
        transport.updateDeliveryStatus(chatId, messageId, DeliveryStatus.READ.name)
    }

    override suspend fun updateDeliveryStatus(
        messageId: String,
        status: DeliveryStatus,
    ): Result<Unit> = runCatching {
        messageDao.updateStatus(messageId, status.name)
    }

    private fun decryptEntity(entity: MessageEntity, me: User?): EncryptedMessage {
        if (me == null) return entity.toDomain(decryptError = "Нет сессии")
        return try {
            // For local relay, use shared AES key instead of RSA
            val sharedKey = sessionKeyManager.getOrCreate(entity.chatId)
            val ciphertext = Base64.getDecoder().decode(entity.ciphertextBase64)
            val iv = Base64.getDecoder().decode(entity.ivBase64)
            hmacManager.verify(ciphertext, hmacManager.deriveHmacKey(sharedKey.encoded), entity.hmacBase64)
            val plainBytes = aesManager.decrypt(ciphertext, sharedKey, iv)

            val text = when (MessageType.valueOf(entity.messageType)) {
                MessageType.TEXT -> String(plainBytes, Charsets.UTF_8)
                MessageType.IMAGE -> "[Изображение ${plainBytes.size} байт]"
            }
            // Calculate isOutgoing dynamically based on current user
            val isOutgoing = entity.senderUid == me.uid
            entity.toDomain(plainText = text, isOutgoing = isOutgoing)
        } catch (e: SignatureVerificationException) {
            // HMAC mismatch - old message encrypted with lost key
            println("DEBUG decryptEntity: HMAC mismatch for message ${entity.id}, key lost")
            entity.toDomain(decryptError = "Сообщение не расшифровано (ключ утерян)")
        } catch (e: DecryptionException) {
            entity.toDomain(decryptError = e.message)
        } catch (e: CryptoException) {
            entity.toDomain(decryptError = e.message)
        } catch (e: Exception) {
            entity.toDomain(decryptError = "Ошибка расшифровки: ${e.message}")
        }
    }

    private suspend fun ingestRemote(dto: RemoteMessageDto, chatId: String, me: User) {
        if (messageDao.getById(dto.id) != null) return
        val senderAvatar = userDao.findByUid(dto.senderUid)?.avatarPath
        val isOutgoing = dto.senderUid == me.uid

        // Debug: log isOutgoing flag
        println("DEBUG ingestRemote: senderUid=${dto.senderUid}, me.uid=${me.uid}, isOutgoing=$isOutgoing")

        // Decrypt message to get actual text for preview
        val sharedKey = sessionKeyManager.getOrCreate(chatId)
        val ciphertext = Base64.getDecoder().decode(dto.message)
        val iv = Base64.getDecoder().decode(dto.iv)
        val preview = try {
            val plainBytes = aesManager.decrypt(ciphertext, sharedKey, iv)
            val messageType = MessageType.valueOf(dto.messageType)
            when (messageType) {
                MessageType.TEXT -> String(plainBytes, Charsets.UTF_8)
                MessageType.IMAGE -> "Фото"
            }
        } catch (e: Exception) {
            println("DEBUG ingestRemote: Failed to decrypt for preview: ${e.message}")
            "Сообщение"
        }

        // Ensure chat exists for recipient
        if (!isOutgoing) {
            val existingChat = chatDao.findExisting(me.uid, dto.senderUid)
            if (existingChat == null) {
                val sender = userDao.findByUid(dto.senderUid)
                if (sender != null) {
                    val chatEntity = fm.mrc.sfeduchat.data.local.entity.ChatEntity(
                        id = chatId,
                        ownerUid = me.uid,
                        participantUid = dto.senderUid,
                        participantUsername = dto.senderUsername,
                        participantAvatarPath = sender.avatarPath,
                        lastMessagePreview = preview,
                        lastMessageTimestamp = dto.timestamp,
                    )
                    chatDao.insert(chatEntity)
                }
            } else {
                // Update preview for existing recipient chat
                chatDao.updatePreviewAll(chatId, preview, dto.timestamp)
            }
        }
        
        messageDao.insert(
            MessageEntity(
                id = dto.id,
                chatId = chatId,
                senderUid = dto.senderUid,
                senderUsername = dto.senderUsername,
                senderAvatarPath = senderAvatar,
                ciphertextBase64 = dto.message,
                ivBase64 = dto.iv,
                encryptedAesKeyBase64 = dto.encryptedAesKey,
                hmacBase64 = dto.hmac,
                timestamp = dto.timestamp,
                isOutgoing = isOutgoing,
                deliveryStatus = dto.deliveryStatus,
                messageType = dto.messageType,
                attachmentMimeType = dto.attachmentMimeType,
            ),
        )
        chatDao.updatePreviewAll(chatId, "Новое сообщение", dto.timestamp)
        transport.updateDeliveryStatus(chatId, dto.id, DeliveryStatus.DELIVERED.name)
    }

    private fun aliasFor(uid: String) = "${KeystoreManager.DEFAULT_KEY_ALIAS}_$uid"

    private fun previewFor(type: MessageType) = when (type) {
        MessageType.TEXT -> "Сообщение"
        MessageType.IMAGE -> "Фото"
    }
}
