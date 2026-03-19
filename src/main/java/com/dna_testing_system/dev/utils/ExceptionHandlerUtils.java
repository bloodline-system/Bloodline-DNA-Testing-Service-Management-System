package com.dna_testing_system.dev.utils;

import com.dna_testing_system.dev.dto.ApiResponse;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

@UtilityClass
@Slf4j
public class ExceptionHandlerUtils {

    public ResponseEntity<ApiResponse<Void>> generateErrorResponse(Exception ex, String exceptionMessage, WebRequest request, HttpStatus status) {
        log.error("Exception caught - status: {}, path: {}, message: {}",
                status.value(),
                request.getDescription(false),
                ex.getMessage());

        ApiResponse<Void> errorResponse = ApiResponse.error(status.value(), exceptionMessage, request.getDescription(false));
        return new ResponseEntity<>(errorResponse, HttpStatusCode.valueOf(status.value()));
    }

    public ResponseEntity<ApiResponse<Void>> generateErrorResponse(Exception ex, WebRequest request, HttpStatus status) {
        log.error("Exception caught - status: {}, path: {}, message: {}",
                status.value(),
                request.getDescription(false),
                ex.getMessage());

        ApiResponse<Void> errorResponse = ApiResponse.error(status.value(), ex.getMessage(), request.getDescription(false));
        return new ResponseEntity<>(errorResponse, HttpStatusCode.valueOf(status.value()));
    }
}
