package com.dna_testing_system.dev.exception;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.utils.ExceptionHandlerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler({
            SignUpNotValidException.class,
            OptFailException.class, AddRoleFailException.class, LoginNotValidException.class,
            InvalidTokenException.class, BlacklistedTokenException.class,
    })
    ResponseEntity<ApiResponse<Void>> handleBadRequestsException(RuntimeException ex, WebRequest request) {
        return ExceptionHandlerUtils.generateErrorResponse(ex, request, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({
            ResourceNotFoundException.class
    })
    ResponseEntity<ApiResponse<Void>> handleNotFoundsException(RuntimeException ex, WebRequest request) {
        return ExceptionHandlerUtils.generateErrorResponse(ex, request, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({
            AuthorizationDeniedException.class
    })
    ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(RuntimeException ex, WebRequest request) {
        return ExceptionHandlerUtils.generateErrorResponse(ex, request, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({
            RuntimeException.class
    })
    ResponseEntity<ApiResponse<Void>> handleServerException(RuntimeException ex, WebRequest request) {
        return ExceptionHandlerUtils.generateErrorResponse(ex, request, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({
            HttpClientErrorException.Unauthorized.class
    })
    ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(RuntimeException ex, WebRequest request) {
        return ExceptionHandlerUtils.generateErrorResponse(ex, request, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({
            HttpClientErrorException.Forbidden.class
    })
    ResponseEntity<ApiResponse<Void>> handleForbiddenException(RuntimeException ex, WebRequest request) {
        return ExceptionHandlerUtils.generateErrorResponse(ex, request, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles validation errors.
     *
     * @param ex the exception
     * @return error response with status 400
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            WebRequest request) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .data(errors)
                .path(request.getDescription(false))
                .timestamp(java.time.Instant.now())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            WebRequest request) {
        return ExceptionHandlerUtils.generateErrorResponse(ex, request, HttpStatus.BAD_REQUEST);
    }
}