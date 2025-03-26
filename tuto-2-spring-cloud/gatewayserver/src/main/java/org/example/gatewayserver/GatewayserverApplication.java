package org.example.gatewayserver;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;

@SpringBootApplication
public class GatewayserverApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayserverApplication.class, args);
    }

    /**
     * Configures the API Gateway routes for EazyBank services using Spring Cloud Gateway.
     * <p>
     * This function defines routing rules for the "Accounts", "Loans", and "Cards" services.
     * Each route:
     * - Matches requests with a specific path prefix (e.g., "/eazybank/accounts/**").
     * - Rewrites the request path by removing the "/eazybank/{service}/" prefix.
     * - Adds a custom response header ("X-Response-Time") to include the timestamp of the request.
     * - Forwards the request to the corresponding microservice using a load balancer (lb://).
     * <p>
     * Additional features for each route:
     * - **Accounts Service**:
     * - Implements a **Circuit Breaker** to handle failures gracefully.
     * - If the service fails, requests are forwarded to a fallback endpoint ("/contactSupport").
     * - **Loans Service**:
     * - Implements a **Retry Mechanism** to handle transient failures.
     * - Retries failed requests up to 3 times with exponential backoff.
     * - **Cards Service**:
     * - Implements **Rate Limiting** to prevent abuse or overloading.
     * - Limits requests to 1 per second per user (identified by the "user" header).
     * <p>
     * This setup enables seamless request forwarding, load balancing, fault tolerance, and rate limiting
     * while keeping URLs clean and ensuring a resilient and scalable system.
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
//                    .requestRateLimiter(config -> config // Configures rate limiting
//                        .setRateLimiter(redisRateLimiter())  // Sets the rate limiter to use the RedisRateLimiter bean
//                        .setKeyResolver(userKeyResolver())   // Sets the key resolver to use the userKeyResolver bean
//                    )
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

    /// - Redis tracks the number of tokens available for each client.
    /// - When a request is made, the rate limiter checks if there are enough tokens available.
    /// - If tokens are available, the request is allowed, and the token count is decremented.
    /// - If no tokens are available, the request is rejected (e.g., with a 429 Too Many Requests response).
//    @Bean
//    public RedisRateLimiter redisRateLimiter() { /// EXPL001
//        return new RedisRateLimiter(
//            1,  // Number of requests allowed per second (replenish rate)
//            1,  // Maximum number of requests allowed in a burst (burst capacity)
//            1   // Number of tokens requested (consumed) per request (requested tokens)
//        );
//    }

    // KeyResolver:
    //     This is used to determine the key for rate limiting.
    //     The key identifies the client making the request (e.g., a user ID, IP address, or API key).
    // How It Works:
    //     The userKeyResolver extracts the user header from the incoming request.
    //     If the user header is present, it uses the value as the key.
    //     If the user header is missing, it defaults to "anonymous".
    // Why Use This?
    //     This allows you to enforce rate limits on a per-user basis.
    //     For example, you can limit each user to 1 request per second, regardless of their IP address or other factors.
//    @Bean
//    KeyResolver userKeyResolver() {
//        return exchange -> Mono.justOrEmpty(
//            exchange
//            .getRequest()
//            .getHeaders()
//            .getFirst("user")  // Extracts the "user" header from the request
//        )
//        .defaultIfEmpty("anonymous");  // Uses "anonymous" as the default key if the "user" header is missing
//    }

}

/// ////////////////////
/// EXPL001
/// ////////////////////
//Example1:
//    replenishRate:1 request per second.
//    burstCapacity: 3 requests.
//    requestedTokens: 1 token per request.
//
//Scenario:
//
//1. Initial State:
//    - The token bucket is full (3 tokens).
//
//2. First Request:
//    - A client makes a request.
//    - 1 token is consumed.
//    - Tokens remaining: 2.
//
//3. Second Request:
//    - Another request is made.
//    - 1 token is consumed.
//    - Tokens remaining: 1.
//
//4. Third Request:
//    - Another request is made.
//    - 1 token is consumed.
//    - Tokens remaining: 0.
//
//5. Fourth Request:
//    - The client makes a fourth request.
//    - The bucket is empty (0 tokens).
//    - The request is rejected with a 429 Too Many Requests response.
//
//6. Token Replenishment:
//    - After 1 second, 1 token is added to the bucket (due to the replenishRate of 1 request per second).
//    - The bucket now has 1 token.
//    - A new request can be processed.
//
//Example2:
//    replenishRate: 1 request per second.
//    burstCapacity: 4 requests.
//    requestedTokens: 2 tokens per request.
//
//Scenario:
//
//1. Initial State:
//    - The token bucket is full (4 tokens).
//
//2. First Request:
//    - A client makes a request.
//    - 2 tokens are consumed.
//    - Tokens remaining: 2.
//
//3. Second Request:
//    - Another request is made.
//    - 2 tokens are consumed.
//    - Tokens remaining: 0.
//
//3. Third Request:
//    - Another request is made.
//    - 2 tokens are required, but the bucket is empty.
//    - The request is rejected with a 429 Too Many Requests response.
//
//4. Token Replenishment:
//    - After 1 second, 1 token is added to the bucket (due to the replenishRate of 1 request per second).
//    - The bucket now has 1 token.
//    - A new request cannot be processed yet (since 2 tokens are required).
//
//5. After 2 Seconds:
//    - Another token is added to the bucket.
//    - The bucket now has 2 tokens.
//    - A new request can be processed (consuming 2 tokens).
