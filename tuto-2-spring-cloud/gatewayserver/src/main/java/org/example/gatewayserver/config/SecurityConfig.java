package org.example.gatewayserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration class for Spring WebFlux (reactive) applications.
 * Configures security rules for the API Gateway, including JWT-based OAuth2 resource server setup.
 */
@Configuration // Marks this class as a Spring configuration class
@EnableWebFluxSecurity // Enables WebFlux security for reactive applications
public class SecurityConfig {

    /**
     * Configures the security filter chain for the application.
     *
     * @param serverHttpSecurity The builder for creating security rules in WebFlux
     * @return Configured SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity serverHttpSecurity) {
        // Configure authorization rules for different endpoints
        serverHttpSecurity.authorizeExchange(
                        exchanges -> exchanges
                                // Allow all GET requests without authentication
                                .pathMatchers(HttpMethod.GET).permitAll()

                                // Require authentication for these specific endpoints:
                                .pathMatchers("/eazybank/accounts/**").authenticated()
                                .pathMatchers("/eazybank/cards/**").authenticated()
                                .pathMatchers("/eazybank/loans/**").authenticated()
                )
                // Configure OAuth2 Resource Server support for JWT validation
                .oauth2ResourceServer(
                        oAuth2ResourceServerSpec -> oAuth2ResourceServerSpec
                                // Enable JWT validation with default settings
                                // (JWKS URI is configured in application.yml)
                                .jwt(Customizer.withDefaults())
                );

        // Disable CSRF protection (common in API gateways that use token-based auth)
        serverHttpSecurity.csrf(csrfSpec -> csrfSpec.disable());

        // Build and return the security filter chain
        return serverHttpSecurity.build();
    }
}


//NOTES
//
//The use of @EnableWebFluxSecurity instead of @EnableWebSecurity is primarily due to the reactive nature of Spring Cloud Gateway
//
//1. Technical Reason: WebFlux vs Servlet Stack
//    @EnableWebFluxSecurity
//    Used for reactive applications built with Spring WebFlux (like Spring Cloud Gateway).
//        Gateway uses Project Reactor (Mono/Flux) and non-blocking I/O.
//        Works with ServerHttpSecurity (reactive equivalent of HttpSecurity).
//    @EnableWebSecurity
//    Used for traditional Servlet-based Spring MVC applications.
//        Works with blocking HttpSecurity.
//        Would cause compatibility issues in a WebFlux app.
//
//2. Why Gateway Uses WebFlux?
//    Spring Cloud Gateway is built on WebFlux because:
//        Performance: Handles high concurrency with non-blocking calls.
//        Scalability: Essential for API gateways that route many microservices.
//        Reactive Ecosystem: Integrates with reactive databases (e.g., MongoDB, Cassandra) and other reactive services.