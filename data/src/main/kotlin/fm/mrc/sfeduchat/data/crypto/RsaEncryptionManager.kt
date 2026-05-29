package fm.mrc.sfeduchat.data.crypto

import fm.mrc.sfeduchat.domain.model.DecryptionException
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import java.util.Base64

/**
 * RSA-2048 OAEP. Параметры согласованы с Android Keystore (SHA-256 + MGF1-SHA-1).
 */
class RsaEncryptionManager {

    fun encryptWithPublicKey(data: ByteArray, publicKey: PublicKey): ByteArray {
        return try {
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SPEC)
            cipher.doFinal(data)
        } catch (e: Exception) {
            throw DecryptionException("Ошибка RSA шифрования AES-ключа", e)
        }
    }

    fun decryptWithPrivateKey(encrypted: ByteArray, privateKey: PrivateKey): ByteArray {
        val attempts = listOf(
            { decryptAttempt(encrypted, privateKey, RSA_TRANSFORMATION, ANDROID_KEYSTORE_PROVIDER) },
            { decryptAttempt(encrypted, privateKey, RSA_TRANSFORMATION, null) },
            { decryptAttempt(encrypted, privateKey, "RSA/ECB/OAEPPadding", ANDROID_KEYSTORE_PROVIDER) },
        )
        var last: Exception? = null
        for (attempt in attempts) {
            try {
                return attempt()
            } catch (e: Exception) {
                last = e
            }
        }
        throw DecryptionException("Ошибка RSA расшифровки AES-ключа", last)
    }

    private fun decryptAttempt(
        encrypted: ByteArray,
        privateKey: PrivateKey,
        transformation: String,
        provider: String?,
    ): ByteArray {
        val cipher = if (provider != null) {
            Cipher.getInstance(transformation, provider)
        } else {
            Cipher.getInstance(transformation)
        }
        cipher.init(Cipher.DECRYPT_MODE, privateKey, OAEP_SPEC)
        return cipher.doFinal(encrypted)
    }

    fun publicKeyFromBase64(base64: String): PublicKey {
        val bytes = Base64.getDecoder().decode(base64)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(spec)
    }

    fun publicKeyToBase64(publicKey: PublicKey): String =
        Base64.getEncoder().encodeToString(publicKey.encoded)

    companion object {
        const val RSA_ALGORITHM = "RSA"
        const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val RSA_KEY_SIZE = 2048

        val OAEP_SPEC: OAEPParameterSpec = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA1,
            PSource.PSpecified.DEFAULT,
        )
    }
}
