package fm.mrc.sfeduchat.data.repository

import fm.mrc.sfeduchat.data.local.SessionPreferences
import fm.mrc.sfeduchat.data.local.dao.UserDao
import fm.mrc.sfeduchat.data.local.entity.UserEntity
import fm.mrc.sfeduchat.data.mapper.toDomain
import fm.mrc.sfeduchat.domain.model.User
import fm.mrc.sfeduchat.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

class AuthRepositoryImpl(
    private val userDao: UserDao,
    private val sessionPreferences: SessionPreferences,
) : AuthRepository {

    override val currentUser: Flow<User?> =
        sessionPreferences.currentUid.map { uid ->
            uid?.let { userDao.findByUid(it)?.toDomain() }
        }

    override suspend fun register(
        username: String,
        password: String,
        avatarPath: String?,
    ): Result<User> = runCatching {
        val normalized = normalizeUsername(username)
        require(isValidUsername(normalized)) { "Никнейм: от 2 до 32 символов" }
        require(password.length >= 4) { "Пароль минимум 4 символа" }
        if (userDao.findByUsername(normalized) != null) {
            error("Пользователь уже существует")
        }
        val uid = UUID.randomUUID().toString()
        val entity = UserEntity(
            uid = uid,
            username = normalized,
            passwordHash = hashPassword(password),
            avatarPath = avatarPath,
        )
        userDao.insert(entity)
        sessionPreferences.setCurrentUid(uid)
        entity.toDomain()
    }

    override suspend fun login(username: String, password: String): Result<User> = runCatching {
        val normalized = normalizeUsername(username)
        val entity = userDao.findByUsername(normalized)
            ?: error("Пользователь не найден")
        if (entity.passwordHash != hashPassword(password)) {
            error("Неверный пароль")
        }
        sessionPreferences.setCurrentUid(entity.uid)
        entity.toDomain()
    }

    override suspend fun logout() {
        println("DEBUG AuthRepository: logout called")
        sessionPreferences.setCurrentUid(null)
    }

    override suspend fun getCurrentUser(): User? {
        val uid = sessionPreferences.currentUid.first() ?: return null
        return userDao.findByUid(uid)?.toDomain()
    }

    override suspend fun updateAvatar(uid: String, avatarPath: String): Result<Unit> = runCatching {
        userDao.updateAvatar(uid, avatarPath)
    }

    private fun normalizeUsername(raw: String): String = raw.trim()

    private fun isValidUsername(name: String): Boolean =
        name.isNotBlank() && name.length in 2..32

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(password.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
