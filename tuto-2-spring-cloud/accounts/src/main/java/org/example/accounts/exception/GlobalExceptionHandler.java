package org.example.accounts.exception;

import org.example.common.dto.ErrorResponseDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
// This method overrides the default behavior of `ResponseEntityExceptionHandler` for handling `MethodArgumentNotValidException`.
// `MethodArgumentNotValidException` is thrown when validation on an argument annotated with `@Valid` fails.
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, // The exception containing validation errors
            HttpHeaders headers,              // HTTP headers for the response
            HttpStatusCode status,            // HTTP status code for the response
            WebRequest request) {             // The current web request

        // Create a Map to store validation errors.(The key is the field name, the value is the validation error msg)
        Map<String, String> validationErrors = new HashMap<>();

        // Retrieve all validation errors from the exception.
        // `ex.getBindingResult().getAllErrors()` returns a list of `ObjectError` objects,
        // which represent all validation errors (both global and field-specific).
        List<ObjectError> validationErrorList = ex.getBindingResult().getAllErrors();

        // Iterate over the list of validation errors.
        validationErrorList.forEach((error) -> {
            // Cast the `ObjectError` to `FieldError` to access field-specific details.
            // `FieldError` contains information about the field that failed validation.
            String fieldName = ((FieldError) error).getField();

            // Get the default validation error message for the field.
            // This message is defined in the validation annotation (e.g., `@NotEmpty(message = "Name cannot be empty")`).
            String validationMsg = error.getDefaultMessage();

            // Add the field name and validation message to the `validationErrors` map.
            validationErrors.put(fieldName, validationMsg);
        });

        // Return a `ResponseEntity` containing the validation errors and a `BAD_REQUEST` status.
        // The `validationErrors` map will be serialized into JSON and sent as the response body.
        return new ResponseEntity<>(validationErrors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGlobalException(Exception exception,
                                                                  WebRequest webRequest) {
        ErrorResponseDto errorResponseDTO = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException exception,
                                                                            WebRequest webRequest) {
        ErrorResponseDto errorResponseDTO = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CustomerAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDto> handleCustomerAlreadyExistsException(CustomerAlreadyExistsException exception,
                                                                                 WebRequest webRequest) {
        ErrorResponseDto errorResponseDTO = new ErrorResponseDto(
                webRequest.getDescription(false), // EXPL001
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.BAD_REQUEST);
    }

    ///  customized function to handle url not found (ex: /api/wrong-url)
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(NoResourceFoundException exception,
                                                                    HttpHeaders headers,
                                                                    HttpStatusCode status,
                                                                    WebRequest webRequest) {
        ErrorResponseDto errorResponseDTO = new ErrorResponseDto(
                webRequest.getDescription(false),
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(errorResponseDTO, HttpStatus.NOT_FOUND);
    }
}

/*
EXPL001:

    Explanation of webRequest.getDescription(boolean includeClientInfo)
        - The getDescription method is part of the WebRequest interface in Spring, and it provides a description of the request.
        - The boolean includeClientInfo parameter determines whether client-specific information (e.g., client IP address, session ID) should be included in the description.

    What happens when you pass false:
        - If you pass false, the description will exclude client-specific information.
        - The returned description typically includes details like:
            * The HTTP method (e.g., GET, POST).
            * The request URI (e.g., /api/customers).
            * Other non-client-specific details about the request.

    What happens when you pass true:
        - If you pass true, the description will include client-specific information.
        - This might include:
            * The client's IP address.
            * Session ID.
            * Other client-related details.

 */
