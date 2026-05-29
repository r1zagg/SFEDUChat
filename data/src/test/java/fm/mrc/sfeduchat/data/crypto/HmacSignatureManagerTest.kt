package fm.mrc.sfeduchat.data.crypto

import fm.mrc.sfeduchat.domain.model.SignatureVerificationException
import org.junit.Assert.assertThrows
import org.junit.Test

class HmacSignatureManagerTest {

    private val manager = HmacSignatureManager()

    @Test
    fun signAndVerify_success() {
        val ciphertext = "encrypted-payload".toByteArray()
        val key = ByteArray(32) { 1 }
        val signature = manager.sign(ciphertext, manager.deriveHmacKey(key))
        manager.verify(ciphertext, manager.deriveHmacKey(key), signature)
    }

    @Test
    fun verify_tamperedCiphertext_fails() {
        val ciphertext = "original".toByteArray()
        val key = manager.deriveHmacKey(ByteArray(32))
        val signature = manager.sign(ciphertext, key)
        assertThrows(SignatureVerificationException::class.java) {
            manager.verify("tampered".toByteArray(), key, signature)
        }
    }
}
