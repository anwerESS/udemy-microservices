package org.example.accounts.service.client;

import org.example.accounts.dto.CardsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("cards")  // Declares this interface as a Feign client for the "cards" service.
// The "cards" service is the name of the remote service registered in the service registry (e.g., Eureka).
public interface CardsFeignClient {

    @GetMapping(
            value = "/api/fetch",  // Specifies the endpoint path on the remote service.
            consumes = "application/json"  // Specifies that the request and response will be in JSON format.
    )
    public ResponseEntity<CardsDto> fetchCardDetails(@RequestParam String mobileNumber);  // Defines a method to call the remote API.
    // The method sends a GET request to `/api/fetch` with a `mobileNumber` query parameter.
    // The response is wrapped in a `ResponseEntity` containing a `CardsDto` object.
}