package com.gym.server.plugins

import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*

fun Application.configureSecurity() {
    val jwtAudience = "gym-system"
    val jwtDomain = "https://gym-system.com"
    val jwtRealm = "gym-system"
    val jwtSecret = System.getenv("JWT_SECRET") ?: "secret"

    authentication {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }

        simple("auth-simple")
    }
}

class SimpleAuthenticationProvider(config: Config) : AuthenticationProvider(config) {
    class Config(name: String) : AuthenticationProvider.Config(name)

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val authHeader = context.call.request.headers["Authorization"]
        if (authHeader != null && authHeader.startsWith("Bearer simple_session_")) {
            val userId = authHeader.removePrefix("Bearer simple_session_")
            // Mock a principal that contains the userId using a real but temporary JWT
            try {
                val tempToken = com.auth0.jwt.JWT.create()
                    .withAudience("gym-system")
                    .withIssuer("https://gym-system.com")
                    .withClaim("userId", userId)
                    .withClaim("role", "MEMBER")
                    .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256("secret"))
                
                val decoded = com.auth0.jwt.JWT.decode(tempToken)
                context.principal(JWTPrincipal(decoded))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

fun AuthenticationConfig.simple(name: String) {
    val provider = SimpleAuthenticationProvider(SimpleAuthenticationProvider.Config(name))
    register(provider)
}
