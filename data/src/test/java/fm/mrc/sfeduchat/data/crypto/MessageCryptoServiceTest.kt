package fm.mrc.sfeduchat.data.crypto

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.junit.Test
import java.security.KeyPairGenerator

class MessageCryptoServiceTest {

    private lateinit var service: MessageCryptoService
    private lateinit var rsa: RsaEncryptionManager
    private lateinit var alice: java.security.KeyPair
    private lateinit var bob: java.security.KeyPair

    @Before
    fun setup() {
        rsa = RsaEncryptionManager()
        val gen = KeyPairGenerator.getInstance("RSA")
        gen.initialize(2048)
        alice = gen.generateKeyPair()
        bob = gen.generateKeyPair()
        val keystore = mockk<KeystoreManager>()
        every { keystore.wrapAesKeyForSelf(any(), any()) } answers {
            rsa.encryptWithPublicKey(firstArg(), alice.public)
        }
        every { keystore.unwrapAesKeyForSelf(any(), any()) } answers {
            rsa.decryptWithPrivateKey(firstArg(), alice.private)
        }
        service = MessageCryptoService(AesEncryptionManager(), rsa, HmacSignatureManager(), keystore)
    }

    @Test
    fun hybridEncryptDecrypt_incoming() {
        val plain = "Привет, Bob!".toByteArray(Charsets.UTF_8)
        val payload = service.encryptForRecipient(plain, bob.public, "alice_alias")
        val decrypted = service.decryptIncoming(
            payload.ciphertextBase64,
            payload.ivBase64,
            payload.encryptedAesKeyBase64,
            payload.hmacBase64,
            bob.private,
        )
        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun hybridEncryptDecrypt_outgoing() {
        val plain = "Моё исходящее".toByteArray(Charsets.UTF_8)
        val payload = service.encryptForRecipient(plain, bob.public, "alice_alias")
        val decrypted = service.decryptOutgoing(
            payload.ciphertextBase64,
            payload.ivBase64,
            payload.encryptedAesKeySelfBase64,
            payload.hmacBase64,
            "alice_alias",
        )
        assertArrayEquals(plain, decrypted)
    }
}
