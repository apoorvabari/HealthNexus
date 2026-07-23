package org.proj.exception;

import org.proj.dto.AccountResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AccountResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        AccountResponse response = new AccountResponse();

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
    public ResponseEntity<AccountResponse> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String defaultMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(org.springframework.validation.FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");

        AccountResponse response = AccountResponse.builder()
                .message(defaultMessage)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<AccountResponse> handleBadCredentialsException(
            org.springframework.security.authentication.BadCredentialsException ex) {

        AccountResponse response = new AccountResponse();
        response.setMessage(ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AccountResponse> handleRuntimeException(
            RuntimeException ex) {

        AccountResponse response = new AccountResponse();

        response.setMessage(ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

    }

}