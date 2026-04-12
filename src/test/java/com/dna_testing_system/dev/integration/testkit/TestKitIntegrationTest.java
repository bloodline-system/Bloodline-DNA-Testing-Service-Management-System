package com.dna_testing_system.dev.integration.testkit;

import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import com.dna_testing_system.dev.entity.TestKit;
import com.dna_testing_system.dev.integration.common.AbstractIntegrationTest;
import com.dna_testing_system.dev.repository.TestKitRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Test Kit Integration Tests")
@WithMockUser(username = "admin", roles = {"ADMIN"})
class TestKitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private TestKitRepository testKitRepository;

    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/v1/test-kits";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testKitRepository.deleteAll();
        flushRedis();
    }

    @Test
    @DisplayName("Create test kit with valid request returns 201")
    void createTestKit_withValidRequest_returnsCreated() throws Exception {
        TestKitRequest request = buildTestKitRequest(
                "Paternity Basic Kit",
                "PATERNITY",
                "BLOOD",
                BigDecimal.valueOf(120.00),
                BigDecimal.valueOf(110.00),
                50,
                "High quality paternity test kit",
                LocalDate.now().plusDays(90),
                "Bloodline Lab",
                true
        );

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Test kit created successfully"));

        assertThat(testKitRepository.count()).isEqualTo(1);
        TestKit saved = testKitRepository.findAll().get(0);
        assertThat(saved.getKitName()).isEqualTo(request.getKitName());
        assertThat(saved.getKitType()).isEqualTo(request.getKitType().name());
        assertThat(saved.getSampleType()).isEqualTo(request.getSampleType().name());
        assertThat(saved.getBasePrice()).isEqualByComparingTo(request.getBasePrice());
        assertThat(saved.getCurrentPrice()).isEqualByComparingTo(request.getCurrentPrice());
        assertThat(saved.getQuantityInStock()).isEqualTo(request.getQuantityInStock());
        assertThat(saved.getExpiryDate()).isEqualTo(request.getExpiryDate());
        assertThat(saved.getProducedBy()).isEqualTo(request.getProducedBy());
        assertThat(saved.getIsAvailable()).isTrue();
    }

    @Test
    @DisplayName("Create test kit with missing required field returns 400")
    void createTestKit_withInvalidRequest_returnsBadRequest() throws Exception {
        String invalidJson = "{"
                + "\"kitType\":\"PATERNITY\"," 
                + "\"sampleType\":\"BLOOD\"," 
                + "\"basePrice\":120," 
                + "\"currentPrice\":110," 
                + "\"quantityInStock\":10," 
                + "\"producedBy\":\"Lab Ltd\"," 
                + "\"expiryDate\":\"" + LocalDate.now().plusDays(30) + "\"}";

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));

        assertThat(testKitRepository.count()).isZero();
    }

    @Test
    @DisplayName("Get test kit by id returns persisted test kit")
    void getTestKitById_returnsOkAndMatchesPersisted() throws Exception {
        TestKit testKit = buildTestKitEntity(
                "Genetic Premium Kit",
                "GENETIC_HEALTH",
                "SALIVA",
                BigDecimal.valueOf(250.00),
                BigDecimal.valueOf(225.00),
                30,
                "Premium genetic testing",
                LocalDate.now().plusDays(120),
                "Bloodline Lab",
                true
        );
        TestKit saved = testKitRepository.save(testKit);

        mockMvc.perform(get(BASE_URL + "/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kit retrieved successfully"))
                .andExpect(jsonPath("$.data.kitName").value(saved.getKitName()))
                .andExpect(jsonPath("$.data.kitType").value(saved.getKitType()))
                .andExpect(jsonPath("$.data.sampleType").value(saved.getSampleType()));
    }

    @Test
    @DisplayName("Update test kit returns 200 and saves changes")
    void updateTestKit_returnsOkAndPersistsChanges() throws Exception {
        TestKit testKit = buildTestKitEntity(
                "Health Kit",
                "GENETIC_HEALTH",
                "SWAB",
                BigDecimal.valueOf(180.00),
                BigDecimal.valueOf(170.00),
                20,
                "Health screening kit",
                LocalDate.now().plusDays(100),
                "Health Labs",
                true
        );
        TestKit saved = testKitRepository.save(testKit);

        TestKitRequest updateRequest = buildTestKitRequest(
                "Health Kit Updated",
                "GENETIC_HEALTH",
                "SWAB",
                BigDecimal.valueOf(190.00),
                BigDecimal.valueOf(175.00),
                15,
                "Updated health screening kit",
                LocalDate.now().plusDays(150),
                "Health Labs",
                true
        );

        mockMvc.perform(put(BASE_URL + "/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kit updated successfully"));

        TestKit updated = testKitRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getKitName()).isEqualTo(updateRequest.getKitName());
        assertThat(updated.getBasePrice()).isEqualByComparingTo(updateRequest.getBasePrice());
        assertThat(updated.getCurrentPrice()).isEqualByComparingTo(updateRequest.getCurrentPrice());
        assertThat(updated.getQuantityInStock()).isEqualTo(updateRequest.getQuantityInStock());
        assertThat(updated.getKitDescription()).isEqualTo(updateRequest.getKitDescription());
    }

    @Test
    @DisplayName("Delete test kit returns 204 and removes entity")
    void deleteTestKit_returnsNoContentAndDeletes() throws Exception {
        TestKit testKit = buildTestKitEntity(
                "Archive Kit",
                "GENETIC_HEALTH",
                "BLOOD",
                BigDecimal.valueOf(99.00),
                BigDecimal.valueOf(89.00),
                10,
                "Archive kit for records",
                LocalDate.now().plusDays(60),
                "Archive Lab",
                true
        );
        TestKit saved = testKitRepository.save(testKit);

        mockMvc.perform(delete(BASE_URL + "/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        assertThat(testKitRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("Search test kits returns matching results")
    void searchTestKits_returnsMatchingResults() throws Exception {
        TestKit first = buildTestKitEntity(
                "Alpha Kit",
                "GENETIC_HEALTH",
                "SALIVA",
                BigDecimal.valueOf(140.00),
                BigDecimal.valueOf(130.00),
                40,
                "Alpha genetics kit",
                LocalDate.now().plusDays(90),
                "Alpha Lab",
                true
        );
        TestKit second = buildTestKitEntity(
                "Beta Kit",
                "GENETIC_HEALTH",
                "BLOOD",
                BigDecimal.valueOf(160.00),
                BigDecimal.valueOf(150.00),
                35,
                "Beta testing kit",
                LocalDate.now().plusDays(90),
                "Beta Lab",
                true
        );
        testKitRepository.save(first);
        testKitRepository.save(second);

        mockMvc.perform(get(BASE_URL + "/search")
                        .param("query", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Test kits found successfully"))
                .andExpect(jsonPath("$.data.content[0].kitName").value("Alpha Kit"));
    }

    private TestKitRequest buildTestKitRequest(
            String kitName,
            String kitType,
            String sampleType,
            BigDecimal basePrice,
            BigDecimal currentPrice,
            Integer quantityInStock,
            String kitDescription,
            LocalDate expiryDate,
            String producedBy,
            Boolean isAvailable
    ) {
        return TestKitRequest.builder()
                .kitName(kitName)
                .kitType(Enum.valueOf(com.dna_testing_system.dev.enums.KitType.class, kitType))
                .sampleType(Enum.valueOf(com.dna_testing_system.dev.enums.SampleType.class, sampleType))
                .basePrice(basePrice)
                .currentPrice(currentPrice)
                .quantityInStock(quantityInStock)
                .kitDescription(kitDescription)
                .expiryDate(expiryDate)
                .producedBy(producedBy)
                .isAvailable(isAvailable)
                .build();
    }

    private TestKit buildTestKitEntity(
            String kitName,
            String kitType,
            String sampleType,
            BigDecimal basePrice,
            BigDecimal currentPrice,
            Integer quantityInStock,
            String kitDescription,
            LocalDate expiryDate,
            String producedBy,
            Boolean isAvailable
    ) {
        return TestKit.builder()
                .kitName(kitName)
                .kitType(kitType)
                .sampleType(sampleType)
                .basePrice(basePrice)
                .currentPrice(currentPrice)
                .quantityInStock(quantityInStock)
                .kitDescription(kitDescription)
                .expiryDate(expiryDate)
                .producedBy(producedBy)
                .isAvailable(isAvailable)
                .build();
    }
}
