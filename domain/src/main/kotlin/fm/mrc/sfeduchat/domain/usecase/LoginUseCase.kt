package fm.mrc.sfeduchat.domain.usecase

import fm.mrc.sfeduchat.domain.model.User
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.repository.KeyRepository

class LoginUseCase(
    private val authRepository: AuthRepository,
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(username: String, password: String): Result<User> {
        return authRepository.login(username, password).onSuccess { user ->
            keyRepository.ensureKeyPairExists()
            keyRepository.publishPublicKey(user.uid)
        }
    }
}
