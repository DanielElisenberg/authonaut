package authonaut.services

import authonaut.models.LoginLog
import authonaut.models.PatchUserRequest
import authonaut.models.RedirectUrl
import authonaut.models.User
import authonaut.repository.LoginLogRepository
import authonaut.repository.RedirectUrlRepository
import authonaut.repository.UserRepository
import jakarta.inject.Singleton
import org.springframework.security.crypto.bcrypt.BCrypt

@Singleton
class ManagementService(
        private val userRepository: UserRepository,
        private val redirectUrlRepository: RedirectUrlRepository,
        private val loginLogRepository: LoginLogRepository,
        private val authService: AuthService
) {

    fun getAllUsers(): List<User> {
        return userRepository.getAllUsers()
    }

    fun addUser(username: String, password: String, role: String = "default"): User {
        val needsPasswordUpdate = true
        val passwordHash = BCrypt.hashpw(password, BCrypt.gensalt())
        return userRepository.insertUser(username, passwordHash, role, needsPasswordUpdate)
    }

    fun getUser(id: Int): User {
        return userRepository.getUser(id)
    }

    fun patchUser(userId: Int, patchUserRequest: PatchUserRequest) {
        if (!patchUserRequest.username.isNullOrEmpty()) {
            userRepository.updateUsername(userId, patchUserRequest.username)
        }
        if (!patchUserRequest.password.isNullOrEmpty()) {
            authService.updatePassword(userId, patchUserRequest.password)
        }
        if (!patchUserRequest.role.isNullOrEmpty()) {
            userRepository.updateUserRole(userId, patchUserRequest.role)
        }
        if (patchUserRequest.reset2fa ?: false) {
            userRepository.reset2fa(userId)
        }
    }

    fun getRole(username: String): String {
        return userRepository.getRole(username)
    }

    fun deleteUser(id: Int) {
        userRepository.deleteUser(id)
    }

    fun changeUserRole(userId: Int, newRole: String) {
        userRepository.updateUserRole(userId, newRole)
    }

    fun getAllRedirectUrls(): List<RedirectUrl> {
        return redirectUrlRepository.getAll()
    }

    fun addRedirectUrl(url: String, isValid: Boolean): RedirectUrl {
        return redirectUrlRepository.add(url, isValid)
    }

    fun updateRedirectUrl(url: String, isValid: Boolean) {
        redirectUrlRepository.update(url, isValid)
    }

    fun deleteRedirectUrl(id: Int) {
        redirectUrlRepository.delete(id)
    }

    fun verifyRedirectUrl(url: String): Boolean {
        return redirectUrlRepository.verify(url)
    }

    fun getLogs(page: Int?, pageSize: Int): List<LoginLog> {
        val pageOrDefault =
                if (page == null) {
                    1
                } else {
                    page
                }
        return loginLogRepository.getAll(pageOrDefault, pageSize)
    }
    fun getLogsForUser(userId: Int, pageSize: Int): List<LoginLog> {
        return loginLogRepository.getForUser(userId, pageSize)
    }
    fun getLogPagesCount(pageSize: Int): Long {
        return loginLogRepository.getAmountofPages(pageSize)
    }
}
