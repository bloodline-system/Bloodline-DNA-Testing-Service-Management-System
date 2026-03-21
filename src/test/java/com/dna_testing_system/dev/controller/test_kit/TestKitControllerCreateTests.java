package com.dna_testing_system.dev.controller.test_kit;

import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestKitController POST endpoint (Create).
 * Tests the createTestKit() method with various scenarios.
 */
@DisplayName("TestKit Controller - Create Tests")
class TestKitControllerCreateTests extends TestKitControllerTestBase {

    @Test
    @DisplayName("Should create test kit with valid request and return 201 CREATED")
    void testCreateValidRequest_Returns201Created() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        doNothing().when(testKitService).CreateTestKit(any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Test kit created successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        // Verify
        verify(testKitService, times(1)).CreateTestKit(any(TestKitRequest.class));
    }

    @Test
    @DisplayName("Should create test kit and call service with correct parameters")
    void testCreateValidRequest_VerifiesServiceCall() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        doNothing().when(testKitService).CreateTestKit(any(TestKitRequest.class));

        // Act
        mockMvc.perform(post(API_BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

        // Assert
        verify(testKitService).CreateTestKit(request);
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when kit name is null")
    void testCreateWithNullKitName_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitName(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        // Verify service was never called
        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when kit name is empty")
    void testCreateWithEmptyKitName_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitName("");

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when kit name exceeds 255 characters")
    void testCreateWithKitNameExceedingMaxLength_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitName("a".repeat(256));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when kit type is null")
    void testCreateWithNullKitType_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitType(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when sample type is null")
    void testCreateWithNullSampleType_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setSampleType(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when base price is null")
    void testCreateWithNullBasePrice_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when base price is negative")
    void testCreateWithNegativeBasePrice_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(new BigDecimal("-10.00"));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when current price is null")
    void testCreateWithNullCurrentPrice_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setCurrentPrice(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when current price is negative")
    void testCreateWithNegativeCurrentPrice_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setCurrentPrice(new BigDecimal("-5.00"));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when quantity is null")
    void testCreateWithNullQuantity_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when quantity is negative")
    void testCreateWithNegativeQuantity_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(-5);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when producer is null")
    void testCreateWithNullProducer_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setProducedBy(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when producer name exceeds 255 characters")
    void testCreateWithProducerExceedingMaxLength_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setProducedBy("a".repeat(256));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when expiry date is in the past")
    void testCreateWithPastExpiryDate_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setExpiryDate(LocalDate.now().minusDays(1));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when description exceeds 1000 characters")
    void testCreateWithDescriptionExceedingMaxLength_Returns400BadRequest() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitDescription("a".repeat(1001));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 201 CREATED with optional description null")
    void testCreateWithNullDescription_Returns201Created() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitDescription(null);
        doNothing().when(testKitService).CreateTestKit(any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService, times(1)).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should return 201 CREATED with isAvailable false")
    void testCreateWithIsAvailableFalse_Returns201Created() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setIsAvailable(false);
        doNothing().when(testKitService).CreateTestKit(any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService, times(1)).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should handle RuntimeException from service and return 500")
    void testCreateWithServiceException_Returns500InternalServerError() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        doThrow(new RuntimeException("Service error")).when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isInternalServerError());

        verify(testKitService, times(1)).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept zero quantity in stock")
    void testCreateWithZeroQuantity_Returns201Created() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(0);
        doNothing().when(testKitService).CreateTestKit(any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService, times(1)).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept zero price")
    void testCreateWithZeroPrice_Returns201Created() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(new BigDecimal("0.00"));
        request.setCurrentPrice(new BigDecimal("0.00"));
        doNothing().when(testKitService).CreateTestKit(any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService, times(1)).CreateTestKit(any());
    }
}
