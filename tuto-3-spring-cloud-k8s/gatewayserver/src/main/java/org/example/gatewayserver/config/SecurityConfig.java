package org.example.gatewayserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

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
                                .pathMatchers("/eazybank/accounts/**").hasRole("ACCOUNTS")
                                .pathMatchers("/eazybank/cards/**").hasRole("CARDS")
                                .pathMatchers("/eazybank/loans/**").hasRole("LOANS")
                )
                // Configure OAuth2 Resource Server support for JWT validation
                .oauth2ResourceServer(
                        oAuth2ResourceServerSpec -> oAuth2ResourceServerSpec
//                                // Enable JWT validation with default settings
//                                // (JWKS URI is configured in application.yml)
//                                .jwt(Customizer.withDefaults())
                                .jwt(jwtSpec -> jwtSpec
                                        // Replace default JWT converter with our Keycloak-aware implementation
                                        // This enables extraction of roles from Keycloak's JWT structure
                                        .jwtAuthenticationConverter(grantedAuthoritiesExtractor())
                                )
                );

        // Disable CSRF protection (common in API gateways that use token-based auth)
        serverHttpSecurity.csrf(csrfSpec -> csrfSpec.disable());

        // Build and return the security filter chain
        return serverHttpSecurity.build();
    }


    /**
     * Creates a JWT authentication converter that extracts and converts Keycloak roles
     * into Spring Security authorities.
     *
     * @return Converter that transforms JWT into AuthenticationToken with authorities
     */
    private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
        // Create standard JWT authentication converter
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

        // Set custom role converter to handle Keycloak's role structure
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

        // Adapt the converter for reactive environment
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);
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