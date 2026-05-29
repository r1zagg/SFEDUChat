package fm.mrc.sfeduchat.data.crypto

import fm.mrc.sfeduchat.domain.model.DecryptionException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Симметричное шифрование AES-256-GCM.
 * GCM даёт конфиденциальность и аутентификацию (тег в конце ciphertext).
 * IV уникален для каждого сообщения — критично для безопасности GCM.
 */
class AesEncryptionManager {

    data class AesEncryptedPayload(
        val ciphertext: ByteArray,
        val iv: ByteArray,
    )

    fun generateAesKey(): SecretKey {
        val keyBytes = ByteArray(AES_KEY_SIZE_BYTES)
        SecureRandom().nextBytes(keyBytes)
        return SecretKeySpec(keyBytes, AES_ALGORITHM)
    }

    fun secretKeyFromBytes(keyBytes: ByteArray): SecretKey =
        SecretKeySpec(keyBytes, AES_ALGORITHM)

    fun encrypt(plainBytes: ByteArray, key: SecretKey): AesEncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plainBytes)
        return AesEncryptedPayload(ciphertext, iv)
    }

    fun decrypt(ciphertext: ByteArray, key: SecretKey, iv: ByteArray): ByteArray {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            throw DecryptionException("Ошибка AES-GCM расшифровки", e)
        }
    }

    companion object {
        const val AES_ALGORITHM = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_SIZE_BYTES = 32
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
