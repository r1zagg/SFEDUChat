package fm.mrc.sfeduchat.data.repository

import fm.mrc.sfeduchat.data.crypto.KeystoreManager
import fm.mrc.sfeduchat.data.crypto.RsaEncryptionManager
import fm.mrc.sfeduchat.data.crypto.SessionKeyManager
import fm.mrc.sfeduchat.data.local.dao.UserDao
import fm.mrc.sfeduchat.data.remote.MessageTransport
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.repository.KeyRepository
class KeyRepositoryImpl(
    private val keystoreManager: KeystoreManager,
    private val rsaManager: RsaEncryptionManager,
    private val transport: MessageTransport,
    private val userDao: UserDao,
    private val authRepository: AuthRepository,
    private val sessionKeyManager: SessionKeyManager,
) : KeyRepository {

    private fun aliasFor(uid: String) = "${KeystoreManager.DEFAULT_KEY_ALIAS}_$uid"

    override suspend fun ensureKeyPairExists(): Result<Unit> = runCatching {
        val uid = authRepository.getCurrentUser()?.uid ?: return@runCatching
        val alias = aliasFor(uid)
        val keyPair = keystoreManager.getOrCreateKeyPair(alias)
        // Only store public key in database if not already set
        val existingKey = userDao.findByUid(uid)?.publicKeyBase64
        if (existingKey.isNullOrBlank()) {
            val publicKeyBase64 = rsaManager.publicKeyToBase64(keyPair.public)
            userDao.updatePublicKey(uid, publicKeyBase64)
        }
    }

    suspend fun ensureKeyPairExistsForUser(uid: String): Result<Unit> = runCatching {
        val alias = aliasFor(uid)
        val keyPair = keystoreManager.getOrCreateKeyPair(alias)
        // Only store public key in database if not already set
        val existingKey = userDao.findByUid(uid)?.publicKeyBase64
        if (existingKey.isNullOrBlank()) {
            val publicKeyBase64 = rsaManager.publicKeyToBase64(keyPair.public)
            userDao.updatePublicKey(uid, publicKeyBase64)
        }
    }

    override suspend fun getPublicKeyBase64(): String? {
        val uid = authRepository.getCurrentUser()?.uid ?: return null
        val publicKey = keystoreManager.getPublicKey(aliasFor(uid))
        return rsaManager.publicKeyToBase64(publicKey)
    }

    override suspend fun regenerateKeyPair(): Result<String> = runCatching {
        val uid = authRepository.getCurrentUser()?.uid ?: error("Не авторизован")
        val pair = keystoreManager.regenerateKeyPair(aliasFor(uid))
        val encoded = rsaManager.publicKeyToBase64(pair.public)
        userDao.updatePublicKey(uid, encoded)
        encoded
    }

    override suspend fun publishPublicKey(uid: String): Result<Unit> = runCatching {
        val publicKey = getPublicKeyBase64() ?: error("Публичный ключ недоступен")
        // Store in local database first
        userDao.updatePublicKey(uid, publicKey)
        // Then publish to transport
        transport.publishPublicKey(uid, publicKey)
    }

    override suspend fun fetchPublicKey(uid: String): Result<String> = runCatching {
        transport.fetchPublicKey(uid)
            ?: userDao.findByUid(uid)?.publicKeyBase64
            ?: error("Публичный ключ собеседника не найден")
    }

    override suspend fun getSessionMessageCount(chatId: String): Int =
        sessionKeyManager.getCount(chatId)

    override suspend fun resetSessionKey(chatId: String) {
        sessionKeyManager.reset(chatId)
    }
}
