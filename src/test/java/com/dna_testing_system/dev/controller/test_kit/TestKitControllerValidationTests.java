package com.dna_testing_system.dev.controller.test_kit;

import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import com.dna_testing_system.dev.enums.KitType;
import com.dna_testing_system.dev.enums.SampleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for TestKitController input validation.
 * Tests all @Valid annotations and constraint validations across all endpoints.
 */
@DisplayName("TestKit Controller - Validation Tests")
class TestKitControllerValidationTests extends TestKitControllerTestBase {

    // ===================== @NotBlank VALIDATION =====================

    @Test
    @DisplayName("Should reject null kitName with validation error")
    void testNullKitName_ValidationFails() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitName(null);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should reject empty kitName with validation error")
    void testEmptyKitName_ValidationFails() throws Exception {
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
    @DisplayName("Should reject whitespace-only kitName with validation error")
    void testWhitespaceOnlyKitName_ValidationFails() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitName("   ");

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should reject null producedBy with validation error")
    void testNullProducedBy_ValidationFails() throws Exception {
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
    @DisplayName("Should reject empty producedBy with validation error")
    void testEmptyProducedBy_ValidationFails() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setProducedBy("");

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    // ===================== @Size VALIDATION =====================

    @Test
    @DisplayName("Should reject kitName exceeding 255 characters")
    void testKitNameExceedingMaxSize_ValidationFails() throws Exception {
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
    @DisplayName("Should accept kitName at exactly 255 characters")
    void testKitNameAt255Characters_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitName("a".repeat(255));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should reject producedBy exceeding 255 characters")
    void testProducedByExceedingMaxSize_ValidationFails() throws Exception {
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
    @DisplayName("Should accept producedBy at exactly 255 characters")
    void testProducedByAt255Characters_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setProducedBy("a".repeat(255));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should reject kitDescription exceeding 1000 characters")
    void testKitDescriptionExceedingMaxSize_ValidationFails() throws Exception {
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
    @DisplayName("Should accept kitDescription at exactly 1000 characters")
    void testKitDescriptionAt1000Characters_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitDescription("a".repeat(1000));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept null kitDescription (optional field)")
    void testNullKitDescription_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitDescription(null);
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    // ===================== @NotNull VALIDATION =====================

    @Test
    @DisplayName("Should reject null kitType with validation error")
    void testNullKitType_ValidationFails() throws Exception {
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
    @DisplayName("Should reject null sampleType with validation error")
    void testNullSampleType_ValidationFails() throws Exception {
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
    @DisplayName("Should reject null basePrice with validation error")
    void testNullBasePrice_ValidationFails() throws Exception {
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
    @DisplayName("Should reject null currentPrice with validation error")
    void testNullCurrentPrice_ValidationFails() throws Exception {
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
    @DisplayName("Should reject null quantityInStock with validation error")
    void testNullQuantityInStock_ValidationFails() throws Exception {
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

    // ===================== @DecimalMin VALIDATION =====================

    @Test
    @DisplayName("Should reject negative basePrice")
    void testNegativeBasePrice_ValidationFails() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(new BigDecimal("-0.01"));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept zero basePrice")
    void testZeroBasePrice_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(new BigDecimal("0.00"));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should reject negative currentPrice")
    void testNegativeCurrentPrice_ValidationFails() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setCurrentPrice(new BigDecimal("-0.01"));

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept zero currentPrice")
    void testZeroCurrentPrice_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setCurrentPrice(new BigDecimal("0.00"));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    // ===================== @Min VALIDATION =====================

    @Test
    @DisplayName("Should reject negative quantityInStock")
    void testNegativeQuantity_ValidationFails() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(-1);

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept zero quantityInStock")
    void testZeroQuantity_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(0);
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept large quantityInStock")
    void testLargeQuantity_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setQuantityInStock(999999);
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    // ===================== @Future VALIDATION =====================

    @Test
    @DisplayName("Should reject past expiryDate")
    void testPastExpiryDate_ValidationFails() throws Exception {
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
    @DisplayName("Should reject today's date as expiryDate")
    void testTodayExpiryDate_ValidationFails() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setExpiryDate(LocalDate.now());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isBadRequest());

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept tomorrow as expiryDate")
    void testTomorrowExpiryDate_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setExpiryDate(LocalDate.now().plusDays(1));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept far future expiryDate")
    void testFarFutureExpiryDate_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setExpiryDate(LocalDate.now().plusYears(10));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    // ===================== @Digits VALIDATION =====================

    @Test
    @DisplayName("Should accept basePrice with 2 decimal places")
    void testBasePriceWith2Decimals_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(new BigDecimal("99.99"));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept currentPrice with 1 decimal place")
    void testCurrentPriceWith1Decimal_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setCurrentPrice(new BigDecimal("39.9"));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept price with no decimal places")
    void testPriceWithNoDecimals_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setBasePrice(new BigDecimal("50"));
        request.setCurrentPrice(new BigDecimal("40"));
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    // ===================== ENUM VALIDATION =====================

    @Test
    @DisplayName("Should accept PATERNITY KitType")
    void testPaternityKitType_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setKitType(KitType.PATERNITY);
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }

    @Test
    @DisplayName("Should accept SALIVA SampleType")
    void testSalivaSampleType_ValidationPasses() throws Exception {
        // Arrange
        TestKitRequest request = buildValidTestKitRequest();
        request.setSampleType(SampleType.SALIVA);
        doNothing().when(testKitService).CreateTestKit(any());

        // Act & Assert
        mockMvc.perform(post(API_BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(request)))
                .andExpect(status().isCreated());

        verify(testKitService).CreateTestKit(any());
    }
}
