package fm.mrc.sfeduchat.data.crypto

import java.util.Base64
import fm.mrc.sfeduchat.domain.model.SignatureVerificationException
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Дополнительная HMAC-SHA256 подпись поверх ciphertext.
 * AES-GCM уже аутентифицирует данные; HMAC демонстрирует гибридный подход для курсовой.
 */
class HmacSignatureManager {

    fun sign(ciphertext: ByteArray, hmacKey: ByteArray): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(hmacKey, HMAC_ALGORITHM))
        val signature = mac.doFinal(ciphertext)
        return Base64.getEncoder().encodeToString(signature)
    }

    fun verify(ciphertext: ByteArray, hmacKey: ByteArray, signatureBase64: String) {
        val expected = sign(ciphertext, hmacKey)
        if (!MessageDigest.isEqual(
                Base64.getDecoder().decode(expected),
                Base64.getDecoder().decode(signatureBase64),
            )
        ) {
            throw SignatureVerificationException("HMAC подпись не совпадает — возможна подмена")
        }
    }

    /** Ключ HMAC выводится из AES-ключа через SHA-256 (для демо, без отдельного секрета). */
    fun deriveHmacKey(aesKeyBytes: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(aesKeyBytes)

    companion object {
        const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
