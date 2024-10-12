package authonaut.controllers

import authonaut.services.AuthService
import authonaut.services.LoginResponse
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.cookie.Cookie
import io.micronaut.views.View
import jakarta.inject.Inject
import java.net.URI

@Controller("/")
class AuthController
@Inject
constructor(
        private val authService: AuthService,
) {
    @Get("/login")
    @Produces(MediaType.TEXT_HTML)
    @View("login.html")
    fun showLoginPage(@QueryValue("redirect") redirect: String?): HttpResponse<Map<String, Any>> {
        if (!authService.verifyRedirectUrl(redirect)) {
            return HttpResponse.seeOther(URI.create("/bad-request"))
        }
        return HttpResponse.ok(mapOf("redirect" to redirect.orEmpty()))
    }

    @Post("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @View("2fa.html")
    fun login(
            username: String,
            password: String,
            otp: String?,
            @QueryValue("redirect") redirect: String?
    ): HttpResponse<Any> {
        val responseData = authService.handleCredentials(username, password, otp, redirect)
        return when (responseData) {
            is LoginResponse.BadCredentials -> {
                HttpResponse.ok<Any>().header("HX-Redirect", "/login")
            }
            is LoginResponse.PromptFor2FA -> {
                HttpResponse.ok(responseData.templateData)
            }
            is LoginResponse.Success -> {
                HttpResponse.ok<Any>()
                        .header("HX-Redirect", "/management/dashboard")
                        .cookie(responseData.authCookie)
            }
            is LoginResponse.SuccessWithRedirect -> {
                val tempToken = responseData.token
                HttpResponse.ok<Any>().header("HX-Redirect", "$redirect?token=$tempToken")
            }
        }
    }

    @Post("/refresh")
    fun refreshToken(request: HttpRequest<*>): HttpResponse<String> {
        // TODO: actual implementation
        val token = request.cookies.get("Authorization")?.value
        if (token == null) {
            return HttpResponse.unauthorized()
        }
        if (!authService.verifyJWT(token)) {
            return HttpResponse.unauthorized()
        }
        return HttpResponse.ok("refreshed").cookie(Cookie.of("Authorization", token))
    }

    @Get("/{token}")
    fun retreiveJWT(@PathVariable token: String): HttpResponse<String> {
        val jwtToken = authService.getJWTByToken(token)
        if (jwtToken.isNullOrBlank()) {
            return HttpResponse.unauthorized()
        }
        return HttpResponse.ok(jwtToken)
    }

    @Get("/bad-request")
    @View("bad-request.html")
    fun badRequest(): HttpResponse<Map<String, Any>> {
        return HttpResponse.ok(emptyMap())
    }
}
