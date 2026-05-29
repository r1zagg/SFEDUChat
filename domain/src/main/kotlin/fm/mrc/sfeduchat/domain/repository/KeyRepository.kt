package fm.mrc.sfeduchat.domain.repository

interface KeyRepository {
    suspend fun ensureKeyPairExists(): Result<Unit>
    suspend fun getPublicKeyBase64(): String?
    suspend fun regenerateKeyPair(): Result<String>
    suspend fun publishPublicKey(uid: String): Result<Unit>
    suspend fun fetchPublicKey(uid: String): Result<String>
    suspend fun getSessionMessageCount(chatId: String): Int
    suspend fun resetSessionKey(chatId: String)
}
