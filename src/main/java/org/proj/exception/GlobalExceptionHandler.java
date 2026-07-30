package org.proj.exception;

import org.proj.dto.RegisterResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RegisterResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        RegisterResponse response = new RegisterResponse();

        response.setMessage(ex.getMessage());

        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex.getMessage() != null) {
            if (ex.getMessage().toLowerCase().contains("exist")) {
                status = HttpStatus.CONFLICT;
            } else if (ex.getMessage().toLowerCase().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            }
        }

        return new ResponseEntity<>(response, status);

    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<RegisterResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String defaultMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");

        RegisterResponse response = RegisterResponse.builder()
                .message(defaultMessage)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<RegisterResponse> handleAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex) {

        RegisterResponse response = new RegisterResponse();
        response.setMessage("Access Denied: " + ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RegisterResponse> handleRuntimeException(
            RuntimeException ex) {

        RegisterResponse response = new RegisterResponse();

        response.setMessage(ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

    }

}