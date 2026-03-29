package com.dna_testing_system.dev.controller.test_kit;

import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
import com.dna_testing_system.dev.service.staff.TestKitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TestKitController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.dna_testing_system.dev.config.ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.dna_testing_system.dev.controller.NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.dna_testing_system.dev.security.JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.dna_testing_system.dev.config.RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class TestKitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestKitService testKitService;

    @Test
    void getAllTestKits_ReturnsOkWithPagedData() throws Exception {
        TestKitResponse kit1 = TestKitResponse.builder()
                .id(1L)
                .kitName("Alpha Kit")
                .kitType("GENETIC")
                .sampleType("BLOOD")
                .basePrice(BigDecimal.valueOf(100))
                .currentPrice(BigDecimal.valueOf(80))
                .quantityInStock(200)
                .isAvailable(true)
                .build();

        TestKitResponse kit2 = TestKitResponse.builder()
                .id(2L)
                .kitName("Beta Kit")
                .kitType("GENETIC")
                .sampleType("SALIVA")
                .basePrice(BigDecimal.valueOf(150))
                .currentPrice(BigDecimal.valueOf(120))
                .quantityInStock(100)
                .isAvailable(true)
                .build();

        when(testKitService.GetTestKitResponseList()).thenReturn(List.of(kit1, kit2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/test-kits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kits retrieved successfully"))
                .andExpect(jsonPath("$.data.content", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].kitName").value("Alpha Kit"));

        verify(testKitService, times(1)).GetTestKitResponseList();
    }

    @Test
    void getTestKitById_ReturnsOk() throws Exception {
        TestKitResponse kit = TestKitResponse.builder()
                .id(5L)
                .kitName("Omega Kit")
                .kitType("GENETIC")
                .sampleType("SWAB")
                .isAvailable(true)
                .build();

        when(testKitService.GetTestKitResponseById(5L)).thenReturn(kit);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/test-kits/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kit retrieved successfully"))
                .andExpect(jsonPath("$.data.kitName").value("Omega Kit"));

        verify(testKitService, times(1)).GetTestKitResponseById(5L);
    }

    @Test
    void getTestKitById_WhenNotFound_ReturnsInternalServerError() throws Exception {
        when(testKitService.GetTestKitResponseById(99L)).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/test-kits/{id}", 99L))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("Not found"));

        verify(testKitService, times(1)).GetTestKitResponseById(99L);
    }

    @Test
    void createTestKit_WithInvalidRequest_ReturnsBadRequest() throws Exception {
        String invalidJson = "{" +
                "\"kitType\":\"PATERNITY\"," +
                "\"sampleType\":\"BLOOD\"," +
                "\"basePrice\":120," +
                "\"currentPrice\":110," +
                "\"quantityInStock\":10," +
                "\"producedBy\":\"Lab Ltd\"," +
                "\"expiryDate\":\"" + LocalDate.now().plusDays(30) + "\"}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/test-kits")
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verify(testKitService, never()).CreateTestKit(any());
    }

    @Test
    void createTestKit_ReturnsCreated() throws Exception {
        TestKitRequest request = TestKitRequest.builder()
                .kitName("New Kit")
                .kitType(com.dna_testing_system.dev.enums.KitType.PATERNITY)
                .sampleType(com.dna_testing_system.dev.enums.SampleType.SALIVA)
                .basePrice(BigDecimal.valueOf(120))
                .currentPrice(BigDecimal.valueOf(110))
                .quantityInStock(10)
                .producedBy("Lab Ltd")
                .expiryDate(LocalDate.now().plusDays(30))
                .build();

        String requestJson = "{" +
                "\"kitName\":\"New Kit\"," +
                "\"kitType\":\"PATERNITY\"," +
                "\"sampleType\":\"SALIVA\"," +
                "\"basePrice\":120," +
                "\"currentPrice\":110," +
                "\"quantityInStock\":10," +
                "\"kitDescription\":\"desc\"," +
                "\"expiryDate\":\"" + LocalDate.now().plusDays(30) + "\"," +
                "\"producedBy\":\"Lab Ltd\",\"isAvailable\":true}";

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/test-kits")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Test kit created successfully"));

        verify(testKitService, times(1)).CreateTestKit(any(TestKitRequest.class));
    }

    @Test
    void updateTestKit_ReturnsOk() throws Exception {
        TestKitRequest request = TestKitRequest.builder()
                .kitName("Updated Kit")
                .kitType(com.dna_testing_system.dev.enums.KitType.PATERNITY)
                .sampleType(com.dna_testing_system.dev.enums.SampleType.BLOOD)
                .basePrice(BigDecimal.valueOf(120))
                .currentPrice(BigDecimal.valueOf(110))
                .quantityInStock(30)
                .producedBy("Lab Ltd")
                .expiryDate(LocalDate.now().plusDays(60))
                .build();

        String updateJson = "{" +
                "\"kitName\":\"Updated Kit\"," +
                "\"kitType\":\"PATERNITY\"," +
                "\"sampleType\":\"BLOOD\"," +
                "\"basePrice\":120," +
                "\"currentPrice\":110," +
                "\"quantityInStock\":30," +
                "\"kitDescription\":\"desc\"," +
                "\"expiryDate\":\"" + LocalDate.now().plusDays(60) + "\"," +
                "\"producedBy\":\"Lab Ltd\",\"isAvailable\":true}";

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/test-kits/{id}", 10L)
                        .contentType("application/json")
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kit updated successfully"));

        verify(testKitService, times(1)).UpdateTestKit(eq(10L), any(TestKitRequest.class));
    }

    @Test
    void deleteTestKit_ReturnsNoContent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/test-kits/{id}", 99L))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$.code").value(204))
                .andExpect(jsonPath("$.message").value("Test kit deleted successfully"));

        verify(testKitService, times(1)).DeleteTestKit(99L);
    }

    @Test
    void searchTestKits_ReturnsPagedResults() throws Exception {
        TestKitResponse kit = TestKitResponse.builder()
                .id(22L)
                .kitName("SearchKit")
                .kitType("GENETIC")
                .sampleType("SALIVA")
                .isAvailable(true)
                .build();

        when(testKitService.searchTestKits("search")).thenReturn(List.of(kit));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/test-kits/search")
                        .param("query", "search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kits found successfully"))
                .andExpect(jsonPath("$.data.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].kitName").value("SearchKit"));

        verify(testKitService, times(1)).searchTestKits("search");
    }
}