package authonaut.models

import io.micronaut.core.annotation.Introspected
import io.micronaut.serde.annotation.Serdeable

@Introspected
@Serdeable
data class AddUserRequest(val username: String, val password: String, val role: String = "default")

@Introspected
@Serdeable
data class PatchUserRequest(
        val username: String?,
        val password: String?,
        val role: String?,
        val reset2fa: Boolean?
)

@Introspected @Serdeable data class AddRedirectUrlRequest(val url: String, val isValid: Boolean)
