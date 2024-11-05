package com.example.travelad.advice;

import com.amadeus.exceptions.ResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
public class ApiAdvice {

    // Handle Amadeus API related errors
    @ExceptionHandler(ResponseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Returns 400 Bad Request
    public ErrorDetail handleApiError(ResponseException ex) {
        return new ErrorDetail("API Error", ex.getMessage());
    }

    // Handle general exceptions
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Returns 500 Internal Server Error
    public ErrorDetail handleGeneralError(Exception ex) {
        return new ErrorDetail("General Error", ex.getMessage());
    }

}
