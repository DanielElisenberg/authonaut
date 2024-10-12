package authonaut.services

import authonaut.repository.LoginLogRepository
import authonaut.repository.RedirectUrlRepository
import authonaut.repository.TokenRepository
import authonaut.repository.UserRepository
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.RandomSecretGenerator
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.micronaut.http.annotation.*
import io.micronaut.http.cookie.Cookie
import jakarta.inject.Singleton
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.collections.emptyMap
import kotlin.text.toByteArray
import org.apache.commons.codec.binary.Base32
import org.springframework.security.crypto.bcrypt.BCrypt

data class UserDetails(val userId: Int, val username: String, val role: String)

val algorithm = Jwts.SIG.RS256
val pair = algorithm.keyPair().build()

sealed class LoginResponse {
    data class PromptFor2FA(val templateData: Map<String, Any>) : LoginResponse()
    data class Success(val authCookie: Cookie) : LoginResponse()
    data class SuccessWithRedirect(val token: String) : LoginResponse()
    object BadCredentials : LoginResponse()
}

@Singleton
class AuthService(
        private val userRepository: UserRepository,
        private val redirectUrlRepository: RedirectUrlRepository,
        private val tokenRepository: TokenRepository,
        private val loginLogRespository: LoginLogRepository
) {
    fun handleCredentials(
            username: String,
            password: String,
            oneTimePassword: String?,
            redirectUrl: String?,
    ): LoginResponse {
        if (!verifyRedirectUrl(redirectUrl)) {
            return LoginResponse.BadCredentials
        }
        val validPassword = verifyPassword(username, password)
        if (oneTimePassword == null) {
            if (validPassword && has2FA(username)) {
                return LoginResponse.PromptFor2FA(emptyMap())
            } else if (validPassword) {
                val secret = generate2FASecret(username)
                val authUri = generateAuthenticatorUri(secret)
                val qrCode = generateQrCode(authUri)
                return LoginResponse.PromptFor2FA(
                        mapOf("qrCodeBase64" to Base64.getEncoder().encodeToString(qrCode))
                )
            } else {
                logLoginAttempt(username, redirectUrl, false)
                return LoginResponse.BadCredentials
            }
        }

        val valid2FA = verify2FACode(username, oneTimePassword)
        if (validPassword && valid2FA) {
            val user = userRepository.getUserByUsername(username)
            val jwtToken = generateJWT(user.id, username, user.role)
            logLoginAttempt(username, redirectUrl, true)
            if (!redirectUrl.isNullOrEmpty()) {
                val token = saveJWT(jwtToken)
                return LoginResponse.SuccessWithRedirect(token)
            } else {
                return LoginResponse.Success(
                        Cookie.of("Authorization", jwtToken)
                                .httpOnly(true)
                                .secure(false)
                                .maxAge(3600)
                )
            }
        } else {
            logLoginAttempt(username, redirectUrl, false)
            return LoginResponse.BadCredentials
        }
    }

    fun verifyRedirectUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) {
            return true
        }
        return redirectUrlRepository.verify(url)
    }

    fun logLoginAttempt(username: String, redirectUrl: String?, successful: Boolean) {
        val userId = userRepository.getUserId(username)
        val redirectId =
                if (redirectUrl.isNullOrEmpty()) {
                    null
                } else {
                    redirectUrlRepository.getRedirectUrlId(redirectUrl)
                }
        loginLogRespository.add(userId, redirectId, System.currentTimeMillis(), successful)
    }

    fun updatePassword(userId: Int, newPassword: String) {
        val newPasswordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        userRepository.updateUserPassword(userId, newPasswordHash)
    }

    fun verifyPassword(username: String, password: String): Boolean {
        val storedHash = userRepository.getHashedPassword(username)
        return BCrypt.checkpw(password, storedHash)
    }

    fun addTwoFactorAuth(username: String, twoFactorSecret: String) {
        userRepository.updateTwoFactorAuth(username, twoFactorSecret)
    }

    fun has2FA(username: String): Boolean {
        val twoFactorSecret = userRepository.get2FactorSecret(username)
        return twoFactorSecret != null
    }

    fun verify2FACode(username: String, otp: String): Boolean {
        val secret = userRepository.get2FactorSecret(username)
        val code =
                GoogleAuthenticator(Base32().encode(secret?.toByteArray()))
                        .generate(Date(System.currentTimeMillis()))
        return code == otp
    }

    fun generate2FASecret(username: String): String {
        val randomSecretGenerator = RandomSecretGenerator()
        val secret = randomSecretGenerator.createRandomSecret(HmacAlgorithm.SHA1)
        userRepository.updateTwoFactorAuth(username, secret.toString())
        return secret.toString()
    }

    fun generateAuthenticatorUri(secret: String): String {
        return GoogleAuthenticator(Base32().encode(secret.toByteArray()))
                .otpAuthUriBuilder()
                .issuer("authonaut")
                .buildToString()
    }

    fun generateQrCode(authUri: String): ByteArray {
        val qrCodeWriter = QRCodeWriter()
        val bitMatrix = qrCodeWriter.encode(authUri, BarcodeFormat.QR_CODE, 200, 200)
        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)
        return outputStream.toByteArray()
    }

    fun generateJWT(userId: Int, username: String, role: String): String {
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("userId", userId)
                .signWith(pair.getPrivate(), algorithm)
                .compact()
    }

    fun verifyJWT(token: String): Boolean {
        try {
            Jwts.parser().verifyWith(pair.getPublic()).build().parseSignedClaims(token)
            return true
        } catch (e: JwtException) {
            return false
        }
    }

    fun parseUserDetails(jwtToken: String): UserDetails {
        val payload =
                Jwts.parser()
                        .verifyWith(pair.getPublic())
                        .build()
                        .parseSignedClaims(jwtToken)
                        .payload
        val role = payload.getValue("role")?.toString() ?: "default"
        val userId = payload.getValue("userId")?.toString()?.toInt()
        // TODO: Handle this error properly
        return UserDetails(userId ?: -1, payload.subject, role)
    }

    fun saveJWT(jwtToken: String): String {
        val shortUUID = UUID.randomUUID().toString().substring(0, 8)
        val timestamp = Instant.now().epochSecond

        tokenRepository.deleteExpiredTokens(timestamp - 120)
        tokenRepository.addToken(shortUUID, jwtToken, timestamp)
        return shortUUID
    }

    fun getJWTByToken(shortUUID: String): String? {
        val timestamp = Instant.now().epochSecond
        tokenRepository.deleteExpiredTokens(timestamp - 120)
        return tokenRepository.getJWTByToken(shortUUID)
    }
}
