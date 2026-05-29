package fm.mrc.sfeduchat.data.crypto

import android.content.Context
import android.content.SharedPreferences
import javax.crypto.SecretKey
import java.util.Base64

/**
 * Ротация сессионного контекста: после N сообщений предлагается сброс счётчика.
 * Каждое сообщение всё равно использует свой AES-ключ (forward secrecy на уровне сообщения).
 * Сессионные ключи теперь сохраняются в SharedPreferences для сохранности при перезапуске.
 */
class SessionKeyManager(
    private val aesManager: AesEncryptionManager,
    private val context: Context,
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("session_keys", Context.MODE_PRIVATE)
    private val counters = mutableMapOf<String, Int>()
    private val sharedKeys = mutableMapOf<String, SecretKey>()

    init {
        loadKeysFromStorage()
    }

    private fun loadKeysFromStorage() {
        val allKeys = prefs.all
        allKeys.forEach { (chatId, keyBase64) ->
            if (keyBase64 is String) {
                try {
                    val keyBytes = Base64.getDecoder().decode(keyBase64)
                    val key = aesManager.secretKeyFromBytes(keyBytes)
                    sharedKeys[chatId] = key
                    counters[chatId] = prefs.getInt("${chatId}_count", 0)
                } catch (e: Exception) {
                    println("DEBUG SessionKeyManager: Failed to load key for $chatId: ${e.message}")
                }
            }
        }
        println("DEBUG SessionKeyManager: Loaded ${sharedKeys.size} session keys from storage")
    }

    private fun saveKeyToStorage(chatId: String, key: SecretKey) {
        val keyBase64 = Base64.getEncoder().encodeToString(key.encoded)
        prefs.edit()
            .putString(chatId, keyBase64)
            .putInt("${chatId}_count", counters[chatId] ?: 0)
            .apply()
    }

    private fun removeKeyFromStorage(chatId: String) {
        prefs.edit()
            .remove(chatId)
            .remove("${chatId}_count")
            .apply()
    }

    fun increment(chatId: String): Int {
        val next = (counters[chatId] ?: 0) + 1
        counters[chatId] = next
        sharedKeys[chatId]?.let { saveKeyToStorage(chatId, it) }
        return next
    }

    fun getCount(chatId: String): Int = counters[chatId] ?: 0

    fun reset(chatId: String) {
        counters[chatId] = 0
        sharedKeys.remove(chatId)
        removeKeyFromStorage(chatId)
    }

    fun shouldRotate(chatId: String, threshold: Int = ROTATION_THRESHOLD): Boolean =
        getCount(chatId) >= threshold

    fun getOrCreate(chatId: String): SecretKey {
        return sharedKeys.getOrPut(chatId) {
            val newKey = aesManager.generateAesKey()
            saveKeyToStorage(chatId, newKey)
            newKey
        }
    }

    companion object {
        const val ROTATION_THRESHOLD = 10
    }
}
