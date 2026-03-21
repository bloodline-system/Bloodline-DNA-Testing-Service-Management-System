package com.dna_testing_system.dev.controller.test_kit;

import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestKitController PUT endpoint (Update).
 * Tests the updateTestKit() method with various scenarios.
 */
@DisplayName("TestKit Controller - Update Tests")
class TestKitControllerUpdateTests extends TestKitControllerTestBase {

    @Test
    @DisplayName("Should update test kit with valid request and return 200 OK")
    void testUpdateValidRequest_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        doNothing().when(testKitService).UpdateTestKit(eq(kitId), any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kit updated successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

        // Verify
        verify(testKitService, times(1)).UpdateTestKit(eq(kitId), any(TestKitRequest.class));
    }

    @Test
    @DisplayName("Should update test kit and call service with correct parameters")
    void testUpdateValidRequest_VerifiesServiceCall() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        doNothing().when(testKitService).UpdateTestKit(eq(kitId), any(TestKitRequest.class));

        // Act
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(request)));

        // Assert
        verify(testKitService).UpdateTestKit(kitId, request);
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when kit name is null")
    void testUpdateWithNullKitName_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitName(null);

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when kit name is empty")
    void testUpdateWithEmptyKitName_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitName("");

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when base price is negative")
    void testUpdateWithNegativeBasePrice_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(new BigDecimal("-10.00"));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when current price is negative")
    void testUpdateWithNegativeCurrentPrice_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setCurrentPrice(new BigDecimal("-5.00"));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when quantity is negative")
    void testUpdateWithNegativeQuantity_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(-5);

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when expiry date is in the past")
    void testUpdateWithPastExpiryDate_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setExpiryDate(LocalDate.now().minusDays(1));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when kit type is null")
    void testUpdateWithNullKitType_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitType(null);

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when sample type is null")
    void testUpdateWithNullSampleType_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setSampleType(null);

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 200 OK when updating with only price changes")
    void testUpdateOnlyPrice_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setCurrentPrice(new BigDecimal("29.99"));
        doNothing().when(testKitService).UpdateTestKit(eq(kitId), any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk());

        verify(testKitService, times(1)).UpdateTestKit(eq(kitId), any());
    }

    @Test
    @DisplayName("Should return 200 OK when updating with only quantity changes")
    void testUpdateOnlyQuantity_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(50);
        doNothing().when(testKitService).UpdateTestKit(eq(kitId), any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk());

        verify(testKitService, times(1)).UpdateTestKit(eq(kitId), any());
    }

    @Test
    @DisplayName("Should return 200 OK when toggling availability")
    void testUpdateAvailability_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setIsAvailable(false);
        doNothing().when(testKitService).UpdateTestKit(eq(kitId), any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk());

        verify(testKitService, times(1)).UpdateTestKit(eq(kitId), any());
    }

    @Test
    @DisplayName("Should accept zero quantity in update")
    void testUpdateWithZeroQuantity_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(0);
        doNothing().when(testKitService).UpdateTestKit(eq(kitId), any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk());

        verify(testKitService, times(1)).UpdateTestKit(eq(kitId), any());
    }

    @Test
    @DisplayName("Should accept zero price in update")
    void testUpdateWithZeroPrice_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(new BigDecimal("0.00"));
        request.setCurrentPrice(new BigDecimal("0.00"));
        doNothing().when(testKitService).UpdateTestKit(eq(kitId), any(TestKitRequest.class));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isOk());

        verify(testKitService, times(1)).UpdateTestKit(eq(kitId), any());
    }

    @Test
    @DisplayName("Should handle service exception and return 500")
    void testUpdateWithServiceException_Returns500InternalServerError() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        doThrow(new RuntimeException("Database error")).when(testKitService).UpdateTestKit(eq(kitId), any());

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isInternalServerError());

        verify(testKitService, times(1)).UpdateTestKit(eq(kitId), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when producer exceeds max length")
    void testUpdateWithProducerExceedingMaxLength_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setProducedBy("a".repeat(256));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when description exceeds max length")
    void testUpdateWithDescriptionExceedingMaxLength_Returns400BadRequest() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitDescription("a".repeat(1001));

        // Act & Assert
        mockMvc.perform(put(API_BASE_URL + "/" + kitId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).UpdateTestKit(anyLong(), any());
    }
}
