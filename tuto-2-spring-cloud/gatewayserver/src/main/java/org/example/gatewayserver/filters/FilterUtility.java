package org.example.gatewayserver.filters;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import org.springframework.http.HttpHeaders;
import java.util.List;

/*
    Purpose:
        This utility class provides methods to retrieve and set the correlation ID in the request headers.
        It also provides a generic method to set any header in the request.
 */

@Component // Marks this class as a Spring component, making it a candidate for auto-detection and dependency injection.
public class FilterUtility {

    // Constant defining the name of the correlation ID header.
    public static final String CORRELATION_ID = "eazybank-correlation-id";

    /**
     * Retrieves the correlation ID from the request headers.
     *
     * @param requestHeaders The HTTP headers from the incoming request.
     * @return The correlation ID if present, otherwise null.
     */
    public String getCorrelationId(HttpHeaders requestHeaders) {
        // Check if the correlation ID header is present in the request headers.
        if (requestHeaders.get(CORRELATION_ID) != null) {
            // Retrieve the list of values for the correlation ID header.
            List<String> requestHeaderList = requestHeaders.get(CORRELATION_ID);
            // Return the first value in the list (assuming there's only one correlation ID).
            return requestHeaderList.stream().findFirst().get();
        } else {
            // Return null if the correlation ID header is not present.
            return null;
        }
    }

    /**
     * Sets a header in the request of the ServerWebExchange.
     *
     * @param exchange The ServerWebExchange object representing the current request and response.
     * @param name     The name of the header to set.
     * @param value    The value of the header to set.
     * @return The modified ServerWebExchange object with the new header.
     */
    public ServerWebExchange setRequestHeader(ServerWebExchange exchange, String name, String value) {
        // Mutate the ServerWebExchange to add the specified header to the request.
        return exchange.mutate()
                .request(exchange.getRequest().mutate().header(name, value).build())
                .build();
    }

    /**
     * Sets the correlation ID in the request headers of the ServerWebExchange.
     *
     * @param exchange      The ServerWebExchange object representing the current request and response.
     * @param correlationId The correlation ID to set in the headers.
     * @return The modified ServerWebExchange object with the correlation ID header.
     */
    public ServerWebExchange setCorrelationId(ServerWebExchange exchange, String correlationId) {
        // Delegates to the setRequestHeader method to set the correlation ID header.
        return this.setRequestHeader(exchange, CORRELATION_ID, correlationId);
    }
}
