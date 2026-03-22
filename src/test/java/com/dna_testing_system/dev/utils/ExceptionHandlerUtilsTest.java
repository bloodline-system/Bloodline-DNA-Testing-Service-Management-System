package com.dna_testing_system.dev.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionHandlerUtilsTest {

    @Mock
    private WebRequest webRequest;

    @Test
    void generateErrorResponse() {
            // Given
            Exception ex = new Exception("Test exception");
            String requestDescription = "uri=/test/path";
            when(webRequest.getDescription(false)).thenReturn(requestDescription);
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

            // When
            var responseEntity = ExceptionHandlerUtils.generateErrorResponse(ex, webRequest, status);

            // Then
            assertNotNull(responseEntity);
            assertEquals(status.value(), responseEntity.getStatusCode().value());
            assertNotNull(responseEntity.getBody());
            assertEquals(500, responseEntity.getBody().getCode());
            assertEquals("Test exception", responseEntity.getBody().getMessage());
            assertEquals(requestDescription, responseEntity.getBody().getPath());
    }
}