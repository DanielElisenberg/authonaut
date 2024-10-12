package authonaut.controllers

import authonaut.models.AddRedirectUrlRequest
import authonaut.models.AddUserRequest
import authonaut.models.PatchUserRequest
import authonaut.models.RedirectUrl
import authonaut.services.AuthService
import authonaut.services.ManagementService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.views.View
import jakarta.inject.Inject
import kotlin.collections.emptyMap

@Controller("/management")
class ManagementController
@Inject
constructor(
        private val managementService: ManagementService,
        private val authService: AuthService,
) {

    @Get("/dashboard")
    @Produces(MediaType.TEXT_HTML)
    @View("dashboard/index.html")
    fun showDashboard(request: HttpRequest<*>): HttpResponse<Map<String, Any>> {
        val role = request.getAttribute("jwtRole", String::class.java).orElse(null)
        val username = request.getAttribute("jwtUsername", String::class.java).orElse(null)
        val userId = request.getAttribute<Int>("jwtUserId", Int::class.java).orElse(null)

        if (role == null || username == null || userId == null) {
            return HttpResponse.ok<Map<String, Any>>(emptyMap()).header("HX-Redirect", "/login")
        }

        val logins = managementService.getLogsForUser(userId, 20)
        return HttpResponse.ok<Map<String, Any>>(
                mapOf(
                        "role" to role,
                        "username" to username,
                        "userId" to userId,
                        "loginHistory" to logins
                )
        )
    }

    @Get("/dashboard/users")
    @Produces(MediaType.TEXT_HTML)
    @View("/dashboard/users.html")
    fun showUsersDashboard(request: HttpRequest<*>): Map<String, Any> {
        val role = request.getAttribute("jwtRole", String::class.java).orElse("default")
        return mapOf("role" to role, "users" to managementService.getAllUsers())
    }

    @Get("/dashboard/users/{id}")
    @Produces(MediaType.TEXT_HTML)
    @View("/dashboard/user-edit.html")
    fun showUserEditDashboard(
            request: HttpRequest<*>,
            @PathVariable id: Int
    ): HttpResponse<Map<String, Any>> {
        val pathUserId = id
        val jwtUserId = request.getAttribute("jwtUserId", Int::class.java).orElse(null)
        val jwtRole = request.getAttribute("jwtRole", String::class.java).orElse(null)
        if (jwtUserId == null || jwtRole == null) {
            return HttpResponse.ok<Map<String, Any>>(emptyMap()).header("HX-Redirect", "/login")
        }
        if (jwtRole != "admin" && jwtUserId != pathUserId) {
            return HttpResponse.ok<Map<String, Any>>(emptyMap()).header("HX-Redirect", "/login")
        }
        val username = managementService.getUser(id).username
        return HttpResponse.ok<Map<String, Any>>(mapOf("userId" to id, "username" to username))
    }

    @Get("/dashboard/redirect-urls")
    @Produces(MediaType.TEXT_HTML)
    @View("dashboard/redirect-urls.html")
    fun showRedirectUrlDashboard(request: HttpRequest<*>): Map<String, Any> {
        val role = request.getAttribute("jwtRole", String::class.java).orElse("default")
        return mapOf("role" to role, "redirectUrls" to managementService.getAllRedirectUrls())
    }

    @Get("/dashboard/logs")
    @Produces(MediaType.TEXT_HTML)
    @View("dashboard/logs.html")
    fun showLogsDashboard(request: HttpRequest<*>, @QueryValue page: Int?): Map<String, Any> {
        val role = request.getAttribute("jwtRole", String::class.java).orElse("default")
        return mapOf(
                "isHtmx" to request.headers.contains("HX-Request"),
                "role" to role,
                "logs" to managementService.getLogs(page, 20),
                "totalPages" to managementService.getLogPagesCount(20)
        )
    }

    @Post("/users")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @View("fragments/added-user.html")
    fun addUser(
            request: HttpRequest<*>,
            @Body addUserRequest: AddUserRequest
    ): HttpResponse<Map<String, Any>> {
        if (request.getAttribute("jwtRole", String::class.java).orElse("default") != "admin") {
            return HttpResponse.unauthorized()
        }
        val user =
                managementService.addUser(
                        addUserRequest.username,
                        addUserRequest.password,
                        addUserRequest.role
                )
        return HttpResponse.ok(mapOf("user" to user))
    }

    @Patch("/users/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun updateUser(
            request: HttpRequest<*>,
            @PathVariable id: Int,
            @Body patchUserRequest: PatchUserRequest
    ): HttpResponse<Any> {
        if (request.getAttribute("jwtRole", String::class.java).orElse("default") != "admin") {
            return HttpResponse.unauthorized()
        }
        managementService.patchUser(id, patchUserRequest)
        return HttpResponse.ok<Any>().header("HX-Redirect", "/management/dashboard/users")
    }

    @Delete("/users/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @View("fragments/deleted-row.html")
    fun deleteUser(request: HttpRequest<*>, @PathVariable id: Int): HttpResponse<Map<String, Any>> {
        if (request.getAttribute("jwtRole", String::class.java).orElse("default") != "admin") {
            return HttpResponse.unauthorized()
        }
        managementService.deleteUser(id)
        return HttpResponse.ok(emptyMap())
    }

    @Post("/redirect-urls")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @View("fragments/added-redirect-url.html")
    fun addRedirectUrl(
            request: HttpRequest<*>,
            @Body addRedirectUrlRequest: AddRedirectUrlRequest
    ): HttpResponse<Map<String, Any>> {
        if (request.getAttribute("jwtRole", String::class.java).orElse("default") != "admin") {
            return HttpResponse.unauthorized()
        }
        val redirectUrl: RedirectUrl =
                managementService.addRedirectUrl(
                        addRedirectUrlRequest.url,
                        addRedirectUrlRequest.isValid,
                )
        return HttpResponse.ok(mapOf("redirectUrl" to redirectUrl))
    }

    @Delete("/redirect-urls/{id}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @View("fragments/deleted-row.html")
    fun deleteRedirectUrl(
            request: HttpRequest<*>,
            @PathVariable id: Int
    ): HttpResponse<Map<String, Any>> {
        if (request.getAttribute("jwtRole", String::class.java).orElse("default") != "admin") {
            return HttpResponse.unauthorized()
        }
        managementService.deleteRedirectUrl(id)
        return HttpResponse.ok(emptyMap())
    }
}
