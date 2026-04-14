package com.dna_testing_system.dev.integration.auth;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.*;

/**
 * Test assertion and utility helper class for authentication integration tests.
 * Reduces boilerplate code for common test operations.
 */
public class AuthTestAssertions {

    private final ObjectMapper objectMapper;

    public AuthTestAssertions(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ==================== Response Extraction ====================

    /**
     * Extracts AuthTokensResponseDTO from MvcResult response body.
     * 
     * @param result MvcResult from MockMvc perform
     * @return AuthTokensResponseDTO containing access and refresh tokens
     */
    public AuthTokensResponseDTO extractAuthTokensResponse(MvcResult result) throws Exception {
        String responseContent = result.getResponse().getContentAsString();
        ApiResponse<AuthTokensResponseDTO> response = objectMapper.readValue(
                responseContent,
                objectMapper.getTypeFactory()
                        .constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class)
        );
        return response.getData();
    }

    /**
     * Extracts generic response data from MvcResult.
     * 
     * @param result MvcResult from MockMvc perform
     * @param dataType Class type of the response data
     * @return Response data of specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T extractResponseData(MvcResult result, Class<T> dataType) throws Exception {
        String responseContent = result.getResponse().getContentAsString();
        ApiResponse<T> response = objectMapper.readValue(
                responseContent,
                objectMapper.getTypeFactory()
                        .constructParametricType(ApiResponse.class, dataType)
        );
        return response.getData();
    }

    // ==================== Token Assertions ====================

    /**
     * Asserts that access and refresh tokens are present and valid.
     * 
     * @param tokens AuthTokensResponseDTO to validate
     */
    public void assertValidTokens(AuthTokensResponseDTO tokens) {
        assertThat(tokens).isNotNull();
        assertThat(tokens.getAccessToken()).isNotEmpty().isNotBlank();
        assertThat(tokens.getRefreshToken()).isNotEmpty().isNotBlank();
    }

    /**
     * Asserts that two tokens are different.
     * Useful for verifying token refresh operations.
     * 
     * @param oldToken Previous token
     * @param newToken New token after refresh
     */
    public void assertTokensAreDifferent(String oldToken, String newToken) {
        assertThat(oldToken).isNotEqualTo(newToken);
    }

    /**
     * Asserts that both tokens are valid and different.
     * 
     * @param oldTokens Previous AuthTokensResponseDTO
     * @param newTokens New AuthTokensResponseDTO after refresh
     */
    public void assertTokensRefreshed(AuthTokensResponseDTO oldTokens, AuthTokensResponseDTO newTokens) {
        assertValidTokens(newTokens);
        assertTokensAreDifferent(oldTokens.getAccessToken(), newTokens.getAccessToken());
    }

    // ==================== HTTP Status Assertions ====================

    /**
     * Asserts common success response codes.
     * 
     * @param statusCode HTTP status code to verify
     */
    public void assertSuccessStatusCode(int statusCode) {
        assertThat(statusCode)
                .as("HTTP status code should be 2xx (success)")
                .isBetween(200, 299);
    }

    /**
     * Asserts common error response codes.
     * 
     * @param statusCode HTTP status code to verify
     */
    public void assertErrorStatusCode(int statusCode) {
        assertThat(statusCode)
                .as("HTTP status code should be 4xx or 5xx (error)")
                .isGreaterThanOrEqualTo(400);
    }

    /**
     * Asserts that status code indicates authentication failure.
     * 
     * @param statusCode HTTP status code to verify
     */
    public void assertAuthenticationFailure(int statusCode) {
        assertThat(statusCode)
                .as("HTTP status code should indicate authentication failure")
                .isIn(401, 403);
    }

    // ==================== Bearer Token Methods ====================

    /**
     * Creates Bearer token header value.
     * 
     * @param accessToken Token to wrap
     * @return Bearer token header value
     */
    public String createBearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    /**
     * Validates Bearer token format.
     * 
     * @param authHeader Authorization header value
     * @return true if valid Bearer token format
     */
    public boolean isValidBearerTokenFormat(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7;
    }

    /**
     * Extracts token from Bearer token header.
     * 
     * @param bearerToken Bearer token header value
     * @return Token without "Bearer " prefix
     */
    public String extractTokenFromBearer(String bearerToken) {
        if (isValidBearerTokenFormat(bearerToken)) {
            return bearerToken.substring(7);
        }
        return "";
    }

    // ==================== Validation Error Assertions ====================

    /**
     * Asserts that response contains validation error.
     * 
     * @param responseContent Response body as string
     * @param fieldName Field that failed validation
     */
    public void assertValidationError(String responseContent, String fieldName) {
        assertThat(responseContent)
                .as("Response should contain validation error for field: " + fieldName)
                .contains(fieldName);
    }

    /**
     * Asserts that response contains specific error message.
     * 
     * @param responseContent Response body as string
     * @param errorMessage Expected error message
     */
    public void assertContainsErrorMessage(String responseContent, String errorMessage) {
        assertThat(responseContent)
                .as("Response should contain error message: " + errorMessage)
                .contains(errorMessage);
    }

    // ==================== Custom Matchers ====================

    /**
     * Checks if response contains JWT token structure.
     * (Very basic check: contains dots as JWT separator)
     * 
     * @param token Token to validate
     * @return true if token has JWT structure
     */
    public boolean hasJwtStructure(String token) {
        return token != null && token.split("\\.").length == 3;
    }

    /**
     * Asserts that token has JWT structure.
     * 
     * @param token Token to validate
     */
    public void assertHasJwtStructure(String token) {
        assertThat(token)
                .as("Token should have JWT structure (three parts separated by dots)")
                .isNotNull()
                .contains(".");
        
        int dotCount = (int) token.chars().filter(c -> c == '.').count();
        assertThat(dotCount)
                .as("JWT token should have exactly 2 dots (3 parts)")
                .isEqualTo(2);
    }

    // ==================== Response Code Assertions ====================

    /**
     * Asserts API response code.
     * 
     * @param responseContent Response body as string
     * @param expectedCode Expected API response code
     */
    public void assertResponseCode(String responseContent, int expectedCode) throws Exception {
        ApiResponse<?> response = objectMapper.readValue(responseContent, ApiResponse.class);
        assertThat(response.getCode())
                .as("API response code should be " + expectedCode)
                .isEqualTo(expectedCode);
    }

    /**
     * Asserts API response message.
     * 
     * @param responseContent Response body as string
     * @param expectedMessage Expected response message
     */
    public void assertResponseMessage(String responseContent, String expectedMessage) throws Exception {
        ApiResponse<?> response = objectMapper.readValue(responseContent, ApiResponse.class);
        assertThat(response.getMessage())
                .as("API response message should match")
                .isEqualTo(expectedMessage);
    }
}
