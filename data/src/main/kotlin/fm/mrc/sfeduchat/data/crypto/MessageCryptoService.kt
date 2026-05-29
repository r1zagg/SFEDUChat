package fm.mrc.sfeduchat.data.crypto

import fm.mrc.sfeduchat.domain.model.DecryptionException
import fm.mrc.sfeduchat.domain.model.MessageType
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Base64
import javax.crypto.SecretKey

/**
 * Гибридная схема: AES-256-GCM для данных + RSA-2048 для обёртки AES-ключа.
 */
class MessageCryptoService(
    private val aesManager: AesEncryptionManager,
    private val rsaManager: RsaEncryptionManager,
    private val hmacManager: HmacSignatureManager,
    private val keystoreManager: KeystoreManager,
) {

    data class EncryptedPayload(
        val ciphertextBase64: String,
        val ivBase64: String,
        val encryptedAesKeyBase64: String,
        val encryptedAesKeySelfBase64: String,
        val hmacBase64: String,
        val messageType: MessageType,
    )

    fun encryptForRecipient(
        plainBytes: ByteArray,
        recipientPublicKey: PublicKey,
        senderKeyAlias: String,
        messageType: MessageType = MessageType.TEXT,
    ): EncryptedPayload {
        val aesKey = aesManager.generateAesKey()
        val encrypted = aesManager.encrypt(plainBytes, aesKey)
        val aesKeyBytes = aesKey.encoded

        val forRecipient = rsaManager.encryptWithPublicKey(aesKeyBytes, recipientPublicKey)
        val forSelf = keystoreManager.wrapAesKeyForSelf(aesKeyBytes, senderKeyAlias)
        val hmacKey = hmacManager.deriveHmacKey(aesKeyBytes)
        val hmac = hmacManager.sign(encrypted.ciphertext, hmacKey)

        return EncryptedPayload(
            ciphertextBase64 = Base64.getEncoder().encodeToString(encrypted.ciphertext),
            ivBase64 = Base64.getEncoder().encodeToString(encrypted.iv),
            encryptedAesKeyBase64 = Base64.getEncoder().encodeToString(forRecipient),
            encryptedAesKeySelfBase64 = Base64.getEncoder().encodeToString(forSelf),
            hmacBase64 = hmac,
            messageType = messageType,
        )
    }

    fun decryptIncoming(
        ciphertextBase64: String,
        ivBase64: String,
        encryptedAesKeyBase64: String,
        hmacBase64: String,
        privateKey: PrivateKey,
    ): ByteArray {
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
        val iv = Base64.getDecoder().decode(ivBase64)
        val wrappedKey = Base64.getDecoder().decode(encryptedAesKeyBase64)

        val aesKeyBytes = rsaManager.decryptWithPrivateKey(wrappedKey, privateKey)
        hmacManager.verify(ciphertext, hmacManager.deriveHmacKey(aesKeyBytes), hmacBase64)

        val aesKey: SecretKey = aesManager.secretKeyFromBytes(aesKeyBytes)
        return aesManager.decrypt(ciphertext, aesKey, iv)
    }

    fun decryptOutgoing(
        ciphertextBase64: String,
        ivBase64: String,
        encryptedAesKeySelfBase64: String,
        hmacBase64: String,
        senderKeyAlias: String,
    ): ByteArray {
        if (encryptedAesKeySelfBase64.isBlank()) {
            throw DecryptionException("Нет ключа для расшифровки исходящего сообщения")
        }
        val ciphertext = Base64.getDecoder().decode(ciphertextBase64)
        val iv = Base64.getDecoder().decode(ivBase64)
        val wrappedKey = Base64.getDecoder().decode(encryptedAesKeySelfBase64)

        val aesKeyBytes = keystoreManager.unwrapAesKeyForSelf(wrappedKey, senderKeyAlias)
        hmacManager.verify(ciphertext, hmacManager.deriveHmacKey(aesKeyBytes), hmacBase64)

        val aesKey: SecretKey = aesManager.secretKeyFromBytes(aesKeyBytes)
        return aesManager.decrypt(ciphertext, aesKey, iv)
    }
}
