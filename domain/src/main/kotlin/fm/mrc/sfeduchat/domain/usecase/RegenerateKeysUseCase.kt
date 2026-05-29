package fm.mrc.sfeduchat.domain.usecase

import fm.mrc.sfeduchat.domain.repository.AuthRepository
import fm.mrc.sfeduchat.domain.repository.KeyRepository

class RegenerateKeysUseCase(
    private val authRepository: AuthRepository,
    private val keyRepository: KeyRepository,
) {
    suspend operator fun invoke(): Result<String> {
        val uid = authRepository.getCurrentUser()?.uid
            ?: return Result.failure(IllegalStateException("Пользователь не авторизован"))
        return keyRepository.regenerateKeyPair().onSuccess {
            keyRepository.publishPublicKey(uid)
        }
    }
}
