package fm.mrc.sfeduchat.domain.repository

import fm.mrc.sfeduchat.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    suspend fun register(username: String, password: String, avatarPath: String? = null): Result<User>
    suspend fun updateAvatar(uid: String, avatarPath: String): Result<Unit>
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout()
    suspend fun getCurrentUser(): User?
}
