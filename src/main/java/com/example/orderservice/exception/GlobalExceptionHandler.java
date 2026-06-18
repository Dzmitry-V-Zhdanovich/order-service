package com.example.orderservice.exception;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getPath(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        return request.getRequestURI();
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {

        String requestId = getRequestId();
        log.warn("Resource not found - RequestId: {}, Path: {}, Message: {}",
                requestId, getPath(request), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(getPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        log.error("Validation error: {}", ex.getMessage());

        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Ошибка валидации входных данных")
                .path(getPath(request))
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(
            FeignException ex, HttpServletRequest request) {

        String requestId = getRequestId();

        HttpStatus status;
        String message;

        if (ex.status() == 404) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "User service is temporarily unavailable. Please try again later.";
            log.error("UserService not available - RequestId: {}, Status: {}", requestId, ex.status());
        } else if (ex.status() >= 500) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "User service is experiencing issues. Please try again later.";
            log.error("UserService error - RequestId: {}, Status: {}", requestId, ex.status());
        } else {
            status = HttpStatus.BAD_GATEWAY;
            message = "Error communicating with user service.";
            log.error("Feign client error - RequestId: {}, Status: {}", requestId, ex.status());
        }

        ErrorResponse error = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(getPath(request))
                .build();

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(UserServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceUnavailable(
            UserServiceUnavailableException ex, HttpServletRequest request) {

        String requestId = getRequestId();
        log.warn("Circuit breaker triggered - RequestId: {}, Path: {}, Message: {}",
                requestId, getPath(request), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("User service is currently unavailable. Order data may be incomplete.")
                .path(getPath(request))
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
}
