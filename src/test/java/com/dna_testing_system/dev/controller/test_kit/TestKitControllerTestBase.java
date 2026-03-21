package com.dna_testing_system.dev.controller.test_kit;

import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
import com.dna_testing_system.dev.enums.KitType;
import com.dna_testing_system.dev.enums.SampleType;
import com.dna_testing_system.dev.service.staff.TestKitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Base test configuration class for TestKitController tests.
 * Provides common test setup, mocks, and helper methods for all test classes.
 */
@ExtendWith(MockitoExtension.class)
public abstract class TestKitControllerTestBase {

    protected MockMvc mockMvc;

    @Mock
    protected TestKitService testKitService;

    protected static final String API_BASE_URL = "/api/v1/test-kits";
    protected static final String CONTENT_TYPE = "application/json";
    
    private final ObjectMapper objectMapper;

    public TestKitControllerTestBase() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestKitController(testKitService))
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .build();
    }

    /**
     * Helper method to build a valid TestKitRequest for testing
     */
    protected TestKitRequest buildValidTestKitRequest() {
        return TestKitRequest.builder()
                .kitName("DNA Test Kit Paternity")
                .kitType(KitType.PATERNITY)
                .sampleType(SampleType.SALIVA)
                .basePrice(new BigDecimal("49.99"))
                .currentPrice(new BigDecimal("39.99"))
                .quantityInStock(100)
                .kitDescription("A standard DNA test kit for paternity testing")
                .expiryDate(LocalDate.now().plusYears(2))
                .producedBy("GeneTech Labs")
                .isAvailable(true)
                .build();
    }

    /**
     * Helper method to build a TestKitRequest with custom values
     */
    protected TestKitRequest buildTestKitRequest(
            String kitName,
            KitType kitType,
            SampleType sampleType,
            BigDecimal basePrice,
            BigDecimal currentPrice,
            Integer quantityInStock,
            LocalDate expiryDate,
            String producedBy) {
        return TestKitRequest.builder()
                .kitName(kitName)
                .kitType(kitType)
                .sampleType(sampleType)
                .basePrice(basePrice)
                .currentPrice(currentPrice)
                .quantityInStock(quantityInStock)
                .kitDescription("Test kit description")
                .expiryDate(expiryDate)
                .producedBy(producedBy)
                .isAvailable(true)
                .build();
    }

    /**
     * Helper method to build a valid TestKitResponse for testing
     */
    protected TestKitResponse buildValidTestKitResponse() {
        return buildTestKitResponse(
                1L,
                "DNA Test Kit Standard",
                new BigDecimal("49.99"),
                new BigDecimal("39.99"),
                100
        );
    }

    /**
     * Helper method to build a TestKitResponse with specific values
     */
    protected TestKitResponse buildTestKitResponse(
            Long id,
            String kitName,
            BigDecimal basePrice,
            BigDecimal currentPrice,
            Integer quantityInStock) {
        return TestKitResponse.builder()
                .id(id)
                .kitName(kitName)
                .kitType("PATERNITY")
                .sampleType("SALIVA")
                .basePrice(basePrice)
                .currentPrice(currentPrice)
                .quantityInStock(quantityInStock)
                .kitDescription("A DNA test kit for genetic testing")
                .expiryDate(LocalDate.now().plusYears(2))
                .producedBy("GeneTech Labs")
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Helper method to get default pagination
     */
    protected Pageable getDefaultPageable() {
        return PageRequest.of(0, 10);
    }

    /**
     * Helper method to convert object to JSON string
     */
    protected String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
