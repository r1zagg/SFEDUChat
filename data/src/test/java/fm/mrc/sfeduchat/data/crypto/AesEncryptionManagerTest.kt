package fm.mrc.sfeduchat.data.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AesEncryptionManagerTest {

    private val manager = AesEncryptionManager()

    @Test
    fun encryptDecrypt_roundTrip() {
        val key = manager.generateAesKey()
        val plain = "Секретное сообщение для курсовой".toByteArray(Charsets.UTF_8)
        val encrypted = manager.encrypt(plain, key)
        val decrypted = manager.decrypt(encrypted.ciphertext, key, encrypted.iv)
        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun encrypt_generatesUniqueIvEachTime() {
        val key = manager.generateAesKey()
        val plain = "test".toByteArray()
        val first = manager.encrypt(plain, key)
        val second = manager.encrypt(plain, key)
        assertFalse(first.iv.contentEquals(second.iv))
        assertNotEquals(
            first.ciphertext.contentToString(),
            second.ciphertext.contentToString(),
        )
    }

    @Test(expected = fm.mrc.sfeduchat.domain.model.DecryptionException::class)
    fun decrypt_wrongKey_fails() {
        val key1 = manager.generateAesKey()
        val key2 = manager.generateAesKey()
        val encrypted = manager.encrypt("data".toByteArray(), key1)
        manager.decrypt(encrypted.ciphertext, key2, encrypted.iv)
    }
}
