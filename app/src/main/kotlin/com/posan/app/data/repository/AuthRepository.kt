package com.posan.app.data.repository

import com.posan.app.data.local.dao.UserDao
import com.posan.app.data.local.entity.UserEntity
import com.posan.app.data.prefs.SessionManager
import com.posan.app.domain.model.UserRole
import com.posan.app.util.Hash
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    fun observeUsers(): Flow<List<UserEntity>> = userDao.observeAll()

    suspend fun seedDefaultAdminIfEmpty() {
        if (userDao.count() == 0) {
            userDao.insert(
                UserEntity(
                    username = "admin",
                    passwordHash = Hash.hashPassword("admin123"),
                    name = "Administrator",
                    role = UserRole.ADMIN.name
                )
            )
        }
    }

    suspend fun login(username: String, password: String): Result<UserEntity> {
        val user = userDao.findByUsername(username.trim().lowercase())
            ?: return Result.failure(IllegalArgumentException("User tidak ditemukan"))
        if (!user.active) return Result.failure(IllegalStateException("User dinonaktifkan"))
        if (!Hash.verify(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Password salah"))
        }
        sessionManager.setSession(user.id, user.name, user.role)
        return Result.success(user)
    }

    suspend fun logout() {
        sessionManager.clear()
    }

    suspend fun createUser(username: String, password: String, name: String, role: UserRole): Result<Long> {
        val cleaned = username.trim().lowercase()
        if (cleaned.isBlank()) return Result.failure(IllegalArgumentException("Username wajib"))
        if (password.length < 4) return Result.failure(IllegalArgumentException("Password minimal 4 karakter"))
        userDao.findByUsername(cleaned)?.let {
            return Result.failure(IllegalArgumentException("Username sudah dipakai"))
        }
        val id = userDao.insert(
            UserEntity(
                username = cleaned,
                passwordHash = Hash.hashPassword(password),
                name = name.trim().ifBlank { cleaned },
                role = role.name
            )
        )
        return Result.success(id)
    }

    suspend fun updateUser(user: UserEntity, newPassword: String?) {
        val updated = if (newPassword.isNullOrBlank()) user else user.copy(passwordHash = Hash.hashPassword(newPassword))
        userDao.update(updated)
    }

    suspend fun deleteUser(user: UserEntity) {
        if (userDao.count() <= 1) throw IllegalStateException("Tidak bisa menghapus user terakhir")
        userDao.delete(user)
    }

    suspend fun findById(id: Long): UserEntity? = userDao.findById(id)
}
