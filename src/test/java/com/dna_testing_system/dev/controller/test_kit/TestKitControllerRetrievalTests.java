package com.dna_testing_system.dev.controller.test_kit;

import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestKitController GET endpoints (Retrieval).
 * Tests the getAllTestKits() and getTestKitById() methods.
 */
@DisplayName("TestKit Controller - Retrieval Tests")
class TestKitControllerRetrievalTests extends TestKitControllerTestBase {

    // ===================== GET ALL TESTS =====================

    @Test
    @DisplayName("Should retrieve all test kits with default pagination and return 200 OK")
    void testGetAllTestKits_Returns200OK() throws Exception {
        // Arrange
        List<TestKitResponse> testKits = List.of(
                buildTestKitResponse(1L, "Kit 1", new BigDecimal("49.99"), new BigDecimal("39.99"), 100),
                buildTestKitResponse(2L, "Kit 2", new BigDecimal("59.99"), new BigDecimal("49.99"), 50)
        );
        when(testKitService.GetTestKitResponseList()).thenReturn(testKits);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kits retrieved successfully"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(2));

        verify(testKitService, times(1)).GetTestKitResponseList();
    }

    @Test
    @DisplayName("Should return empty list when no test kits exist")
    void testGetAllTestKits_EmptyList_Returns200OK() throws Exception {
        // Arrange
        when(testKitService.GetTestKitResponseList()).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(testKitService).GetTestKitResponseList();
    }

    @Test
    @DisplayName("Should retrieve first page with custom page size")
    void testGetAllTestKits_WithCustomPageSize_Returns200OK() throws Exception {
        // Arrange
        List<TestKitResponse> testKits = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            testKits.add(buildTestKitResponse((long) i, "Kit " + i, new BigDecimal("49.99"), 
                    new BigDecimal("39.99"), 100 * i));
        }
        when(testKitService.GetTestKitResponseList()).thenReturn(testKits);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(5))
                .andExpect(jsonPath("$.data.pageSize").value(5));

        verify(testKitService).GetTestKitResponseList();
    }

    @Test
    @DisplayName("Should retrieve second page correctly")
    void testGetAllTestKits_SecondPage_Returns200OK() throws Exception {
        // Arrange
        List<TestKitResponse> testKits = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            testKits.add(buildTestKitResponse((long) i, "Kit " + i, new BigDecimal("49.99"), 
                    new BigDecimal("39.99"), 100 * i));
        }
        when(testKitService.GetTestKitResponseList()).thenReturn(testKits);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumber").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(25));

        verify(testKitService).GetTestKitResponseList();
    }

    @Test
    @DisplayName("Should retrieve with default pagination when no params provided")
    void testGetAllTestKits_DefaultPagination_Returns200OK() throws Exception {
        // Arrange
        List<TestKitResponse> testKits = List.of(
                buildTestKitResponse(1L, "Kit 1", new BigDecimal("49.99"), new BigDecimal("39.99"), 100)
        );
        when(testKitService.GetTestKitResponseList()).thenReturn(testKits);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(10));

        verify(testKitService).GetTestKitResponseList();
    }

    @Test
    @DisplayName("Should handle service exception in get all")
    void testGetAllTestKits_ServiceException_Returns500InternalServerError() throws Exception {
        // Arrange
        when(testKitService.GetTestKitResponseList()).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL))
                .andExpect(status().isInternalServerError());

        verify(testKitService).GetTestKitResponseList();
    }

    // ===================== GET BY ID TESTS =====================

    @Test
    @DisplayName("Should retrieve test kit by valid ID and return 200 OK")
    void testGetTestKitById_ValidId_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitResponse response = buildValidTestKitResponse();
        when(testKitService.GetTestKitResponseById(kitId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/" + kitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kit retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.kitName").value("DNA Test Kit Standard"))
                .andExpect(jsonPath("$.data.basePrice").value(49.99))
                .andExpect(jsonPath("$.data.currentPrice").value(39.99))
                .andExpect(jsonPath("$.data.quantityInStock").value(100));

        verify(testKitService, times(1)).GetTestKitResponseById(kitId);
    }

    @Test
    @DisplayName("Should retrieve test kit with ID = 1")
    void testGetTestKitById_FirstKit_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitResponse response = buildTestKitResponse(1L, "First Kit", new BigDecimal("49.99"), 
                new BigDecimal("39.99"), 100);
        when(testKitService.GetTestKitResponseById(kitId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/" + kitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1L));

        verify(testKitService).GetTestKitResponseById(kitId);
    }

    @Test
    @DisplayName("Should retrieve test kit with large ID number")
    void testGetTestKitById_LargeId_Returns200OK() throws Exception {
        // Arrange
        Long kitId = 999999L;
        TestKitResponse response = buildTestKitResponse(kitId, "Large ID Kit", new BigDecimal("49.99"), 
                new BigDecimal("39.99"), 100);
        when(testKitService.GetTestKitResponseById(kitId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/" + kitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(999999L));

        verify(testKitService).GetTestKitResponseById(kitId);
    }

    @Test
    @DisplayName("Should retrieve multiple test kits by ID sequentially")
    void testGetTestKitById_MultipleIds_AllSucceed() throws Exception {
        // Arrange
        for (long i = 1; i <= 3; i++) {
            TestKitResponse response = buildTestKitResponse(i, "Kit " + i, new BigDecimal("49.99"), 
                    new BigDecimal("39.99"), 100);
            when(testKitService.GetTestKitResponseById(i)).thenReturn(response);
        }

        // Act & Assert
        for (long i = 1; i <= 3; i++) {
            mockMvc.perform(get(API_BASE_URL + "/" + i))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(i));
        }

        // Verify all calls were made
        for (long i = 1; i <= 3; i++) {
            verify(testKitService).GetTestKitResponseById(i);
        }
    }

    @Test
    @DisplayName("Should include all required fields in response")
    void testGetTestKitById_ResponseContainsAllFields_Success() throws Exception {
        // Arrange
        Long kitId = 1L;
        TestKitResponse response = buildValidTestKitResponse();
        when(testKitService.GetTestKitResponseById(kitId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/" + kitId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.kitName").exists())
                .andExpect(jsonPath("$.data.kitType").exists())
                .andExpect(jsonPath("$.data.sampleType").exists())
                .andExpect(jsonPath("$.data.basePrice").exists())
                .andExpect(jsonPath("$.data.currentPrice").exists())
                .andExpect(jsonPath("$.data.quantityInStock").exists())
                .andExpect(jsonPath("$.data.expiryDate").exists())
                .andExpect(jsonPath("$.data.producedBy").exists())
                .andExpect(jsonPath("$.data.isAvailable").exists());

        verify(testKitService).GetTestKitResponseById(kitId);
    }

    @Test
    @DisplayName("Should handle service exception when retrieving by ID")
    void testGetTestKitById_ServiceException_Returns500InternalServerError() throws Exception {
        // Arrange
        Long kitId = 1L;
        when(testKitService.GetTestKitResponseById(kitId)).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/" + kitId))
                .andExpect(status().isInternalServerError());

        verify(testKitService).GetTestKitResponseById(kitId);
    }

    @Test
    @DisplayName("Should verify service called with correct ID parameter")
    void testGetTestKitById_VerifiesCorrectIdPassed() throws Exception {
        // Arrange
        Long kitId = 42L;
        TestKitResponse response = buildTestKitResponse(kitId, "Test Kit 42", new BigDecimal("49.99"), 
                new BigDecimal("39.99"), 100);
        when(testKitService.GetTestKitResponseById(kitId)).thenReturn(response);

        // Act
        mockMvc.perform(get(API_BASE_URL + "/" + kitId));

        // Assert
        verify(testKitService).GetTestKitResponseById(42L);
        verify(testKitService, never()).GetTestKitResponseById(41L);
        verify(testKitService, never()).GetTestKitResponseById(43L);
    }

    @Test
    @DisplayName("Should return correct structure for paginated response")
    void testGetAllTestKits_ResponseStructure_IsValid() throws Exception {
        // Arrange
        List<TestKitResponse> testKits = List.of(
                buildValidTestKitResponse()
        );
        when(testKitService.GetTestKitResponseList()).thenReturn(testKits);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageNumber").isNumber())
                .andExpect(jsonPath("$.data.pageSize").isNumber())
                .andExpect(jsonPath("$.data.totalElements").isNumber())
                .andExpect(jsonPath("$.data.totalPages").isNumber());

        verify(testKitService).GetTestKitResponseList();
    }
}
