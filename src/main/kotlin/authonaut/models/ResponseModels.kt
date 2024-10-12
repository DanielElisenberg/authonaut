package authonaut.models

import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class RedirectUrl(
        val id: Int,
        val url: String,
        val isValid: Boolean,
)

@Serdeable
data class User(
        val id: Int,
        val username: String,
        val role: String,
        val needsPasswordUpdate: Boolean,
        val hasTwoFactorAuth: Boolean
)

@Serdeable
data class LoginLog(
        val id: Int,
        val username: String,
        val redirectUrl: String,
        val successful: Boolean,
        val timestamp: String
)
