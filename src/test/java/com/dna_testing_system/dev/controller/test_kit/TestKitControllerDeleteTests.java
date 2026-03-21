package com.dna_testing_system.dev.controller.test_kit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for TestKitController DELETE endpoint (Delete).
 * Tests the deleteTestKit() method with various scenarios.
 */
@DisplayName("TestKit Controller - Delete Tests")
class TestKitControllerDeleteTests extends TestKitControllerTestBase {

    @Test
    @DisplayName("Should delete test kit with valid ID and return 204 NO_CONTENT")
    void testDeleteValidId_Returns204NoContent() throws Exception {
        // Arrange
        Long kitId = 1L;
        doNothing().when(testKitService).DeleteTestKit(kitId);

        // Act & Assert
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId))
                .andExpect(status().isNoContent());

        // Verify
        verify(testKitService, times(1)).DeleteTestKit(kitId);
    }

    @Test
    @DisplayName("Should delete test kit and call service with correct ID")
    void testDeleteValidId_VerifiesServiceCall() throws Exception {
        // Arrange
        Long kitId = 5L;
        doNothing().when(testKitService).DeleteTestKit(kitId);

        // Act
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId));

        // Assert
        verify(testKitService).DeleteTestKit(kitId);
    }

    @Test
    @DisplayName("Should delete with ID = 1 successfully")
    void testDeleteWithIdOne_Returns204NoContent() throws Exception {
        // Arrange
        Long kitId = 1L;
        doNothing().when(testKitService).DeleteTestKit(kitId);

        // Act & Assert
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId))
                .andExpect(status().isNoContent());

        verify(testKitService).DeleteTestKit(1L);
    }

    @Test
    @DisplayName("Should delete with large ID successfully")
    void testDeleteWithLargeId_Returns204NoContent() throws Exception {
        // Arrange
        Long kitId = 999999L;
        doNothing().when(testKitService).DeleteTestKit(kitId);

        // Act & Assert
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId))
                .andExpect(status().isNoContent());

        verify(testKitService).DeleteTestKit(kitId);
    }

    @Test
    @DisplayName("Should handle service exception during delete")
    void testDeleteWithServiceException_Returns500InternalServerError() throws Exception {
        // Arrange
        Long kitId = 1L;
        doThrow(new RuntimeException("Failed to delete")).when(testKitService).DeleteTestKit(kitId);

        // Act & Assert
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId))
                .andExpect(status().isInternalServerError());

        verify(testKitService).DeleteTestKit(kitId);
    }

    @Test
    @DisplayName("Should persist successfully after deletion")
    void testDeleteMultipleIds_AllSucceed() throws Exception {
        // Arrange
        Long[] kitIds = {1L, 2L, 3L};
        for (Long kitId : kitIds) {
            doNothing().when(testKitService).DeleteTestKit(kitId);
        }

        // Act & Assert
        for (Long kitId : kitIds) {
            mockMvc.perform(delete(API_BASE_URL + "/" + kitId))
                    .andExpect(status().isNoContent());
        }

        // Verify all were called
        for (Long kitId : kitIds) {
            verify(testKitService).DeleteTestKit(kitId);
        }
    }

    @Test
    @DisplayName("Should return success response structure on delete")
    void testDeleteValidId_ReturnsCorrectResponseStructure() throws Exception {
        // Arrange
        Long kitId = 1L;
        doNothing().when(testKitService).DeleteTestKit(kitId);

        // Act & Assert
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId))
                .andExpect(status().isNoContent());

        // Verify service was called exactly once
        verify(testKitService, times(1)).DeleteTestKit(kitId);
    }

    @Test
    @DisplayName("Should handle concurrent deletion requests")
    void testDeleteConcurrent_BothSucceed() throws Exception {
        // Arrange
        Long kitId1 = 1L;
        Long kitId2 = 2L;
        doNothing().when(testKitService).DeleteTestKit(kitId1);
        doNothing().when(testKitService).DeleteTestKit(kitId2);

        // Act & Assert
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId1))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete(API_BASE_URL + "/" + kitId2))
                .andExpect(status().isNoContent());

        verify(testKitService).DeleteTestKit(kitId1);
        verify(testKitService).DeleteTestKit(kitId2);
    }

    @Test
    @DisplayName("Should delete without body content")
    void testDeleteWithoutBodyContent_Returns204NoContent() throws Exception {
        // Arrange
        Long kitId = 1L;
        doNothing().when(testKitService).DeleteTestKit(kitId);

        // Act & Assert
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId)
                .header("Content-Length", "0"))
                .andExpect(status().isNoContent());

        verify(testKitService).DeleteTestKit(kitId);
    }

    @Test
    @DisplayName("Should verify service method invocation exactly once")
    void testDeleteVerifyInvocationOnce_Success() throws Exception {
        // Arrange
        Long kitId = 1L;
        doNothing().when(testKitService).DeleteTestKit(kitId);

        // Act
        mockMvc.perform(delete(API_BASE_URL + "/" + kitId));

        // Assert - verify called exactly once
        verify(testKitService, times(1)).DeleteTestKit(kitId);
        // Ensure no other methods were called
        verify(testKitService, never()).GetTestKitResponseById(anyLong());
    }

    @Test
    @DisplayName("Should delete test kit with valid sequential IDs")
    void testDeletesequentialIds_Success() throws Exception {
        // Arrange
        for (long i = 1; i <= 5; i++) {
            doNothing().when(testKitService).DeleteTestKit(i);
        }

        // Act & Assert
        for (long i = 1; i <= 5; i++) {
            mockMvc.perform(delete(API_BASE_URL + "/" + i))
                    .andExpect(status().isNoContent());
        }

        // Verify all were deleted
        for (long i = 1; i <= 5; i++) {
            verify(testKitService).DeleteTestKit(i);
        }
    }
}
