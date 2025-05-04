package org.example.gatewayserver.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

/*
    Purpose:
        This class defines a global filter that processes the response after the request has been handled by the filter chain.
        It ensures that the correlation ID from the request headers is added to the response headers, enabling end-to-end tracing in a microservices architecture
 */

@Configuration // Marks this class as a configuration class in Spring. It can define beans and other configuration settings.
public class ResponseTraceFilter {

    // Logger instance to log messages for this class.
    private static final Logger logger = LoggerFactory.getLogger(ResponseTraceFilter.class);

    final FilterUtility filterUtility;

    public ResponseTraceFilter(FilterUtility filterUtility) {
        this.filterUtility = filterUtility;
    }

    /**
     * Defines a global filter bean that processes the response after the request has been handled.
     *
     * @return A GlobalFilter that adds the correlation ID to the response headers.
     */
    @Bean // Marks this method as a bean definition. The returned object will be registered as a Spring bean.
    public GlobalFilter postGlobalFilter() {
        // Return a GlobalFilter implementation using a lambda expression.
        return (exchange, chain) -> {
            // Continue the filter chain and process the request.
            return chain.filter(exchange)
                    // After the request is processed, execute the following logic.
                    .then(Mono.fromRunnable(() -> {
                        // Retrieve the request headers.
                        HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
                        // Get the correlation ID from the request headers.
                        String correlationId = filterUtility.getCorrelationId(requestHeaders);
                        // Log the correlation ID being added to the response headers.
                        logger.debug("Updated the correlation id to the outbound headers: {}", correlationId);
                        // Add the correlation ID to the response headers.
                        exchange.getResponse().getHeaders().add(filterUtility.CORRELATION_ID, correlationId);
                    }));
        };
        /// The filter uses chain.filter(exchange) to continue the filter chain and process the request.
        ///
        /// The then(Mono.fromRunnable(...)) method is used to execute logic after the request has been processed.
        ///
        /// Mono.fromRunnable is used to perform a task (adding the correlation ID to the response headers) that doesn’t return a value but needs to run asynchronously.
    }
}