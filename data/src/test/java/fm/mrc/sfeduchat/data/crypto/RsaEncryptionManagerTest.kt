package fm.mrc.sfeduchat.data.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator

/**
 * JVM-тест RSA без Android Keystore (программная пара ключей).
 */
class RsaEncryptionManagerTest {

    private lateinit var rsaManager: RsaEncryptionManager
    private lateinit var keyPair: java.security.KeyPair

    @Before
    fun setup() {
        rsaManager = RsaEncryptionManager()
        val gen = KeyPairGenerator.getInstance(RsaEncryptionManager.RSA_ALGORITHM)
        gen.initialize(RsaEncryptionManager.RSA_KEY_SIZE)
        keyPair = gen.generateKeyPair()
    }

    @Test
    fun wrapAesKey_roundTrip() {
        val aesKey = ByteArray(32) { it.toByte() }
        val wrapped = rsaManager.encryptWithPublicKey(aesKey, keyPair.public)
        val unwrapped = rsaManager.decryptWithPrivateKey(wrapped, keyPair.private)
        assertArrayEquals(aesKey, unwrapped)
    }

    @Test
    fun publicKeyBase64_roundTrip() {
        val encoded = rsaManager.publicKeyToBase64(keyPair.public)
        val restored = rsaManager.publicKeyFromBase64(encoded)
        val data = "session-key".toByteArray()
        val cipher = rsaManager.encryptWithPublicKey(data, restored)
        val plain = rsaManager.decryptWithPrivateKey(cipher, keyPair.private)
        assertArrayEquals(data, plain)
    }
}
