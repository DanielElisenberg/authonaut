package authonaut.events

import authonaut.config.Environment
import authonaut.repository.UserRepository
import authonaut.repository.initDatabase
import authonaut.services.ManagementService
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Singleton

@Singleton
class StartupEvent(
        private val environment: Environment,
        private val userRepository: UserRepository,
        private val managementService: ManagementService
) : ApplicationEventListener<ServerStartupEvent> {

    override fun onApplicationEvent(event: ServerStartupEvent) {
        initDatabase()
        if (userRepository.getAdminCount() == 0) {
            managementService.addUser(environment.adminUsername, environment.adminPassword, "admin")
        }
    }
}
