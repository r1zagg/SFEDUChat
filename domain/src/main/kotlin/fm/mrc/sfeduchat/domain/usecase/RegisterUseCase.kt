package fm.mrc.sfeduchat.domain.usecase

import fm.mrc.sfeduchat.domain.model.User
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.repository.KeyRepository

class RegisterUseCase(
    private val authRepository: AuthRepository,
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(
        username: String,
        password: String,
        avatarPath: String? = null,
    ): Result<User> {
        return authRepository.register(username, password, avatarPath).onSuccess { user ->
            keyRepository.ensureKeyPairExists()
            keyRepository.publishPublicKey(user.uid)
        }
    }
}
