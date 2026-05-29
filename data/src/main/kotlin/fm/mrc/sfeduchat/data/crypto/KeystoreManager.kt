package fm.mrc.sfeduchat.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import fm.mrc.sfeduchat.domain.model.CryptoException
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey

/**
 * RSA-2048 в Android Keystore. Обёртка AES для себя — через [RsaEncryptionManager] + экспорт публичного ключа.
 */
class KeystoreManager(
    private val rsaManager: RsaEncryptionManager,
) {

    fun getOrCreateKeyPair(alias: String): KeyPair {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            val entry = keyStore.getEntry(alias, null) as KeyStore.PrivateKeyEntry
            return KeyPair(entry.certificate.publicKey, entry.privateKey)
        }
        return generateKeyPair(alias)
    }

    fun regenerateKeyPair(alias: String): KeyPair {
        deleteKey(alias)
        return generateKeyPair(alias)
    }

    fun deleteKey(alias: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    fun getPrivateKey(alias: String): PrivateKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(alias, null) as? KeyStore.PrivateKeyEntry
            ?: throw CryptoException("Ключ $alias не найден в Keystore")
        return entry.privateKey
    }

    fun getPublicKey(alias: String): PublicKey = getOrCreateKeyPair(alias).public

    /** Шифруем AES-ключ экспортированным публичным ключом (совместимо с Keystore). */
    fun wrapAesKeyForSelf(aesKeyBytes: ByteArray, alias: String): ByteArray =
        rsaManager.encryptWithPublicKey(aesKeyBytes, getPublicKey(alias))

    fun unwrapAesKeyForSelf(wrapped: ByteArray, alias: String): ByteArray =
        rsaManager.decryptWithPrivateKey(wrapped, getPrivateKey(alias))

    private fun generateKeyPair(alias: String): KeyPair {
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_RSA,
            ANDROID_KEYSTORE,
        )
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setKeySize(RsaEncryptionManager.RSA_KEY_SIZE)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
            .setUserAuthenticationRequired(false)
            .build()
        generator.initialize(spec)
        return generator.generateKeyPair()
    }

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "sfedu_rsa_key"
    }
}
