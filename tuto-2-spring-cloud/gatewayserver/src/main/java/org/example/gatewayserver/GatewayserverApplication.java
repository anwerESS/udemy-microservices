package org.example.gatewayserver;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootApplication
public class GatewayserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayserverApplication.class, args);
    }

    /**
     * Configures the API Gateway routes for EazyBank services using Spring Cloud Gateway.
     *
     * This function defines routing rules for the "Accounts", "Loans", and "Cards" services.
     * Each route:
     * - Matches requests with a specific path prefix (e.g., "/eazybank/accounts/**").
     * - Rewrites the request path by removing the "/eazybank/{service}/" prefix.
     * - Adds a custom response header ("X-Response-Time") to include the timestamp of the request.
     * - Forwards the request to the corresponding microservice using a load balancer (lb://).
     *
     * This setup enables seamless request forwarding and load balancing while keeping URLs clean.
     */
    @Bean
    public RouteLocator eazyBankRouteConfig(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
            .route(p -> p
                .path("/eazybank/accounts/**")
                .filters(f -> f.rewritePath(
                        "/eazybank/accounts/(?<segment>.*)",
                                "/${segment}"
                        )
                        .addResponseHeader( // Adds a custom response header
                                "X-Response-Time",
                                LocalDateTime.now().toString()
                        )
                        .circuitBreaker(config -> config
                                .setName("accountsCircuitBreaker")  // Sets a unique name for the Circuit Breaker
                                .setFallbackUri("forward:/contactSupport")  // Defines the fallback URI to call when the Circuit Breaker is open or the service fails
                        )
                )
                .uri("lb://ACCOUNTS"))
            .route(p -> p
                .path("/eazybank/loans/**")
                .filters(f -> f.rewritePath(
                        "/eazybank/loans/(?<segment>.*)",
                                "/${segment}"
                        )
                        .addResponseHeader(
                                "X-Response-Time",
                                LocalDateTime.now().toString()
                        )
                        // Configures a retry mechanism
                        .retry(retryConfig -> retryConfig
                                .setRetries(3) // Sets the number of retry attempts to 3
                                .setMethods(HttpMethod.GET) // Applies retry only for GET requests
                                .setBackoff(
                                        Duration.ofMillis(100), // Initial backoff delay of 100ms
                                        Duration.ofMillis(1000), // Max backoff delay of 1000ms
                                        2, // Exponential backoff factor of 2
                                        true // Enables jitter to randomize the backoff time
                                )
                        )
                )
                .uri("lb://LOANS"))
            .route(p -> p
                .path("/eazybank/cards/**")
                .filters(f -> f.rewritePath(
                        "/eazybank/cards/(?<segment>.*)",
                                "/${segment}"
                        )
                        .addResponseHeader( // Adds a custom response header
                                "X-Response-Time",
                                LocalDateTime.now().toString()
                        )
                )
                .uri("lb://CARDS"))
            .build();
    }

    @Bean  // Marks this method as a Spring bean, making it part of the Spring context
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        // Returns a Customizer that configures the default settings for all circuit breakers
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())  // Uses default Circuit Breaker configuration
                .timeLimiterConfig(TimeLimiterConfig.custom()  // Configures the TimeLimiter
                        .timeoutDuration(Duration.ofSeconds(4))  // Sets a timeout of 4 seconds (with this, circuit breaker will wait for 4 seconds)
                        .build())
                .build());
    }

}
