package org.example.gatewayserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

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

}
