package org.example.gatewayserver.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/*
    Purpose: This filter is used to ensure that every incoming request has a unique correlation ID.
    If the ID is already present in the request headers, it logs the ID.
    If not, it generates a new one and sets it in the request.
 */

@Order(1) // Specifies the order in which this filter should be executed relative to other filters. Lower values have higher priority.
@Component // Marks this class as a Spring component, making it a candidate for auto-detection and dependency injection.
public class RequestTraceFilter implements GlobalFilter {

    // Logger instance to log messages for this class.
    private static final Logger logger = LoggerFactory.getLogger(RequestTraceFilter.class);

    final FilterUtility filterUtility;

    public RequestTraceFilter(FilterUtility filterUtility) {
        this.filterUtility = filterUtility;
    }

    // Override the filter method from the GlobalFilter interface.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Retrieve the headers from the incoming HTTP request.
        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();

        // Check if the correlation ID is present in the request headers.
        if (isCorrelationIdPresent(requestHeaders)) {
            // If present, log the correlation ID.
            logger.debug("eazyBank-correlation-id found in RequestTraceFilter : {}",
                    filterUtility.getCorrelationId(requestHeaders));
        } else {
            // If not present, generate a new correlation ID.
            String correlationID = generateCorrelationId();
            // Set the correlation ID in the exchange object.
            exchange = filterUtility.setCorrelationId(exchange, correlationID);
            // Log the newly generated correlation ID.
            logger.debug("eazyBank-correlation-id generated in RequestTraceFilter : {}", correlationID);
        }

        // Continue the filter chain with the modified exchange object.
        return chain.filter(exchange);
    }

    // Helper method to check if a correlation ID is present in the request headers.
    private boolean isCorrelationIdPresent(HttpHeaders requestHeaders) {
        // Return true if the correlation ID is not null, otherwise return false.
        return filterUtility.getCorrelationId(requestHeaders) != null;
    }

    // Helper method to generate a unique correlation ID using UUID.
    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString();
    }
}


/*
    Mono<Void> is a reactive type used to represent an asynchronous operation that doesn’t return a value.
 */