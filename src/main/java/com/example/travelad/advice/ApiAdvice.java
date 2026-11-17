package com.example.travelad.advice;

import com.example.travelad.exceptions.ExternalApiException;
import com.example.travelad.exceptions.InvalidInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiAdvice extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiAdvice.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorDetail> handleResponseStatus(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        return ResponseEntity.status(status).body(errorBody(status));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorDetail> handleInvalidInput(InvalidInputException ex) {
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ErrorDetail> handleExternalApi(ExternalApiException ex) {
        log.warn("External API failure ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(errorBody(HttpStatus.BAD_GATEWAY));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetail> handleGeneralError(Exception ex) {
        log.error("Unhandled API exception ({})", ex.getClass().getName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        // Preserve Spring's status and headers, such as Allow for HTTP 405.
        return super.handleExceptionInternal(
                ex,
                errorBody(status),
                headers,
                status,
                request
        );
    }

    private ErrorDetail errorBody(HttpStatusCode status) {
        String description = status.is5xxServerError()
                ? "The request could not be completed. Please try again later."
                : "The request could not be processed. Please check your input.";

        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        return new ErrorDetail(knownStatus == null ? "HTTP " + status.value()
                : knownStatus.getReasonPhrase(), description);
    }
}