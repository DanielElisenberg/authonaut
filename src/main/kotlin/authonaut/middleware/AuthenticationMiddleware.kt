package authonaut.middleware

import authonaut.services.AuthService
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import jakarta.inject.Singleton
import java.net.URI
import org.reactivestreams.Publisher

@Singleton
@Filter("/management/**")
class AuthenticationMiddleware(private val authService: AuthService) : HttpServerFilter {

    override fun doFilter(
            request: HttpRequest<*>,
            chain: ServerFilterChain
    ): Publisher<MutableHttpResponse<*>> {
        val authToken = request.cookies.get("Authorization")
        if (authToken == null) {
            return Publishers.just(HttpResponse.redirect<Any>(URI("/login")))
        }
        val authTokenString = authToken.value
        if (!authService.verifyJWT(authTokenString)) {
            return Publishers.just(HttpResponse.redirect<Any>(URI("/login")))
        }
        val userDetails = authService.parseUserDetails(authTokenString)
        val updatedRequest =
                request.setAttribute("jwtUsername", userDetails.username)
                        .setAttribute("jwtRole", userDetails.role)
                        .setAttribute("jwtUserId", userDetails.userId)
        return chain.proceed(updatedRequest)
    }

    override fun getOrder(): Int {
        return 0
    }
}
