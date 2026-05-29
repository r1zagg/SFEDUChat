package fm.mrc.sfeduchat.domain.model

open class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause)

open class DecryptionException(message: String, cause: Throwable? = null) : CryptoException(message, cause)

open class SignatureVerificationException(message: String) : CryptoException(message)
