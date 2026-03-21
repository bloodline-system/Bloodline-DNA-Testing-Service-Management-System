package com.dna_testing_system.dev.controller.test_kit;

import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestKitController search endpoint.
 * Tests the searchTestKits() method with various search queries and pagination scenarios.
 */
@DisplayName("TestKit Controller - Search Tests")
class TestKitControllerSearchTests extends TestKitControllerTestBase {

    @Test
    @DisplayName("Should search test kits with valid query and return 200 OK")
    void testSearchValidQuery_Returns200OK() throws Exception {
        // Arrange
        String query = "DNA";
        List<TestKitResponse> results = List.of(
                buildTestKitResponse(1L, "DNA Test Kit Standard", new BigDecimal("49.99"), 
                        new BigDecimal("39.99"), 100)
        );
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kits found successfully"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].kitName").value("DNA Test Kit Standard"));

        verify(testKitService, times(1)).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search with single character query")
    void testSearchSingleCharacterQuery_Returns200OK() throws Exception {
        // Arrange
        String query = "A";
        List<TestKitResponse> results = List.of(
                buildTestKitResponse(1L, "DNA Test Kit Standard", new BigDecimal("49.99"), 
                        new BigDecimal("39.99"), 100)
        );
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search with long query string")
    void testSearchLongQueryString_Returns200OK() throws Exception {
        // Arrange
        String query = "This is a very long search query for testing purposes";
        List<TestKitResponse> results = List.of();
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search with special characters in query")
    void testSearchWithSpecialCharactersQuery_Returns200OK() throws Exception {
        // Arrange
        String query = "test-kit_123";
        List<TestKitResponse> results = List.of();
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk());

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should return empty results when no matches found")
    void testSearchNoMatches_ReturnsEmptyResults() throws Exception {
        // Arrange
        String query = "NonexistentKit";
        when(testKitService.searchTestKits(query)).thenReturn(new ArrayList<>());

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search with pagination - first page")
    void testSearchWithFirstPage_Returns200OK() throws Exception {
        // Arrange
        String query = "Kit";
        List<TestKitResponse> results = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            results.add(buildTestKitResponse((long) i, "Test Kit " + i, new BigDecimal("49.99"), 
                    new BigDecimal("39.99"), 100 * i));
        }
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(10));

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search with pagination - second page")
    void testSearchWithSecondPage_Returns200OK() throws Exception {
        // Arrange
        String query = "Kit";
        List<TestKitResponse> results = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            results.add(buildTestKitResponse((long) i, "Test Kit " + i, new BigDecimal("49.99"), 
                    new BigDecimal("39.99"), 100 * i));
        }
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumber").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(25));

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search with custom page size")
    void testSearchWithCustomPageSize_Returns200OK() throws Exception {
        // Arrange
        String query = "DNA";
        List<TestKitResponse> results = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            results.add(buildTestKitResponse((long) i, "DNA Kit " + i, new BigDecimal("49.99"), 
                    new BigDecimal("39.99"), 100 * i));
        }
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(20));

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should return results with pagination metadata")
    void testSearchResponseContainsPaginationMetadata_Success() throws Exception {
        // Arrange
        String query = "Test";
        List<TestKitResponse> results = List.of(
                buildTestKitResponse(1L, "Test Kit 1", new BigDecimal("49.99"), new BigDecimal("39.99"), 100)
        );
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageNumber").exists())
                .andExpect(jsonPath("$.data.pageSize").exists())
                .andExpect(jsonPath("$.data.totalElements").exists())
                .andExpect(jsonPath("$.data.totalPages").exists())
                .andExpect(jsonPath("$.data.last").exists());

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should verify service called with exact search query")
    void testSearchVerifiesServiceCallWithQuery_Success() throws Exception {
        // Arrange
        String query = "StandardKit";
        List<TestKitResponse> results = List.of();
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act
        mockMvc.perform(get(API_BASE_URL + "/search")
                .param("query", query));

        // Assert
        verify(testKitService).searchTestKits("StandardKit");
        verify(testKitService, never()).searchTestKits("Standard");
        verify(testKitService, never()).searchTestKits("Kit");
    }

    @Test
    @DisplayName("Should handle multiple search results with correct ordering")
    void testSearchMultipleResults_DisplayCorrectly() throws Exception {
        // Arrange
        String query = "Kit";
        List<TestKitResponse> results = List.of(
                buildTestKitResponse(1L, "Kit A", new BigDecimal("49.99"), new BigDecimal("39.99"), 100),
                buildTestKitResponse(2L, "Kit B", new BigDecimal("59.99"), new BigDecimal("49.99"), 50),
                buildTestKitResponse(3L, "Kit C", new BigDecimal("69.99"), new BigDecimal("59.99"), 30)
        );
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].kitName").value("Kit A"))
                .andExpect(jsonPath("$.data.content[1].kitName").value("Kit B"))
                .andExpect(jsonPath("$.data.content[2].kitName").value("Kit C"));

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should handle service exception during search")
    void testSearchWithServiceException_Returns500InternalServerError() throws Exception {
        // Arrange
        String query = "Test";
        when(testKitService.searchTestKits(query)).thenThrow(new RuntimeException("Search failed"));

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isInternalServerError());

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search with default pagination when not specified")
    void testSearchDefaultPagination_Returns200OK() throws Exception {
        // Arrange
        String query = "Standard";
        List<TestKitResponse> results = List.of(
                buildValidTestKitResponse()
        );
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(10));

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search with case-sensitive query")
    void testSearchCaseSensitiveQuery_Returns200OK() throws Exception {
        // Arrange
        String query = "DNA";
        List<TestKitResponse> results = List.of();
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act
        mockMvc.perform(get(API_BASE_URL + "/search")
                .param("query", query));

        // Assert
        verify(testKitService).searchTestKits("DNA");
    }

    @Test
    @DisplayName("Should search with numeric characters in query")
    void testSearchNumericQuery_Returns200OK() throws Exception {
        // Arrange
        String query = "123";
        List<TestKitResponse> results = List.of();
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk());

        verify(testKitService).searchTestKits(query);
    }

    @Test
    @DisplayName("Should search and display all fields in results")
    void testSearchResultsContainAllFields_Success() throws Exception {
        // Arrange
        String query = "Kit";
        List<TestKitResponse> results = List.of(
                buildValidTestKitResponse()
        );
        when(testKitService.searchTestKits(query)).thenReturn(results);

        // Act & Assert
        mockMvc.perform(get(API_BASE_URL + "/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").exists())
                .andExpect(jsonPath("$.data.content[0].kitName").exists())
                .andExpect(jsonPath("$.data.content[0].basePrice").exists())
                .andExpect(jsonPath("$.data.content[0].currentPrice").exists())
                .andExpect(jsonPath("$.data.content[0].quantityInStock").exists());

        verify(testKitService).searchTestKits(query);
    }
}
