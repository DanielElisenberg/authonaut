package authonaut.config

import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Singleton
class Environment {
    @Value("\${env.adminUsername}") lateinit var adminUsername: String
    @Value("\${env.adminPassword}") lateinit var adminPassword: String
}
