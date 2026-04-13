package com.dna_testing_system.dev.integration.feedback;

import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.request.RespondFeedbackRequest;
import com.dna_testing_system.dev.entity.CustomerFeedback;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.enums.ServiceCategory;
import com.dna_testing_system.dev.integration.common.AbstractIntegrationTest;
import com.dna_testing_system.dev.repository.CustomerFeedbackRepository;
import com.dna_testing_system.dev.repository.MedicalServiceRepository;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Feedback Management - Integration")
@Transactional
class FeedbackManagementIntegrationTest extends AbstractIntegrationTest {

    private static final String CUSTOMER_USERNAME = "it_feedback_user";
    private static final String CUSTOMER_EMAIL = "it_feedback_user@example.com";

    private static final String ADMIN_USERNAME = "it_admin_user";
    private static final String ADMIN_EMAIL = "it_admin_user@example.com";

    @DynamicPropertySource
    static void disableRedisPublishing(DynamicPropertyRegistry registry) {
        // Avoid Redis stream listener + JPA listener publishing when running integration tests locally.
        registry.add("app.redis.enabled", () -> false);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;

    @Autowired
    private CustomerFeedbackRepository customerFeedbackRepository;

    /**
     * Prevent Redis stream listener from starting during @SpringBootTest.
     */
    @MockitoBean
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> mailStreamListenerContainer;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        ensureUserWithProfile(CUSTOMER_USERNAME, "Integration", "Customer", CUSTOMER_EMAIL);
        ensureUserWithProfile(ADMIN_USERNAME, "Integration", "Admin", ADMIN_EMAIL);
    }

    private User ensureUserWithProfile(String username, String firstName, String lastName, String email) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = userRepository.save(User.builder()
                    .username(username)
                    .passwordHash("test-hash")
                    .isActive(true)
                    .build());
        }

        if (user.getProfile() == null) {
            UserProfile profile = UserProfile.builder()
                    .user(user)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phoneNumber("0123456789")
                    .build();
            userProfileRepository.save(profile);
            user.setProfile(profile);
            userRepository.save(user);
        }

        return userRepository.findByUsername(username).orElseThrow();
    }

    private MedicalService createMedicalService(String serviceName) {
        MedicalService service = MedicalService.builder()
                .serviceName(serviceName)
                .serviceCategory(ServiceCategory.CIVIL)
                .participants(1)
                .executionTimeDays(3)
                .basePrice(new BigDecimal("100.00"))
                .currentPrice(new BigDecimal("120.00"))
                .isAvailable(true)
                .isBestValue(false)
                .serviceDescription("Integration test service")
                .build();

        return medicalServiceRepository.save(service);
    }

    @Test
    @WithMockUser(username = CUSTOMER_USERNAME, roles = "CUSTOMER")
    void createFeedbackJson_and_viewMyFeedback_succeeds() throws Exception {
        MedicalService service = createMedicalService("IT Service " + UUID.randomUUID());

        String title = "IT Feedback " + UUID.randomUUID();
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .serviceId(service.getId())
                .customerId("temp")
                .serviceQualityRating(5)
                .staffBehaviorRating(4)
                .timelinessRating(3)
                .feedbackTitle(title)
                .feedbackContent("Everything looks good")
                .responseRequired(true)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/feedback/{feedbackId}", "fb1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Feedback submitted successfully!"))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        long feedbackId = root.path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get feedback list successfully"))
                .andExpect(jsonPath("$.data[*].id", hasItem((int) feedbackId)));

        CustomerFeedback saved = customerFeedbackRepository.findById(feedbackId).orElseThrow();
        assertThat(saved.getOverallRating()).isEqualTo((5 + 4 + 3) / 3.0f);
        assertThat(saved.getService()).isNotNull();
        assertThat(saved.getService().getId()).isEqualTo(service.getId());
        assertThat(saved.getResponseRequired()).isTrue();
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = "ADMIN")
    void adminRespondToFeedback_and_filterByStatus_succeeds() throws Exception {
        User customer = userRepository.findByUsername(CUSTOMER_USERNAME).orElseThrow();
        User admin = userRepository.findByUsername(ADMIN_USERNAME).orElseThrow();

        MedicalService service = createMedicalService("IT Service " + UUID.randomUUID());
        String title = "Need response " + UUID.randomUUID();

        CustomerFeedback feedback = CustomerFeedback.builder()
                .customer(customer)
                .service(service)
                .feedbackTitle(title)
                .feedbackContent("Please respond")
                .serviceQualityRating(5)
                .staffBehaviorRating(5)
                .timelinessRating(5)
                .overallRating(5.0f)
                .responseRequired(true)
                .build();
        feedback = customerFeedbackRepository.save(feedback);

        // Before responding: should appear in unresponded list (filtered by title search)
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .param("search", title)
                        .param("responseStatus", "unresponded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.feedbackList[0].id").value(feedback.getId()));

        RespondFeedbackRequest respond = RespondFeedbackRequest.builder()
                .responseContent("Thanks for your feedback")
                .build();

        mockMvc.perform(patch("/api/v1/admin/feedback/{feedbackId}/respond", feedback.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(respond)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Response submitted successfully"))
                .andExpect(jsonPath("$.data.id").value(feedback.getId()))
                .andExpect(jsonPath("$.data.responseContent").value("Thanks for your feedback"))
                .andExpect(jsonPath("$.data.respondedAt").exists())
                .andExpect(jsonPath("$.data.respondedByName").value("Integration Admin"));

        CustomerFeedback reloaded = customerFeedbackRepository.findById(feedback.getId()).orElseThrow();
        assertThat(reloaded.getRespondedAt()).isNotNull();
        assertThat(reloaded.getRespondedBy()).isNotNull();
        assertThat(reloaded.getRespondedBy().getId()).isEqualTo(admin.getId());
        assertThat(reloaded.getResponseContent()).isEqualTo("Thanks for your feedback");

        // After responding: should appear in responded list
        mockMvc.perform(get("/api/v1/admin/feedback")
                        .param("search", title)
                        .param("responseStatus", "responded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.feedbackList[0].id").value(feedback.getId()));
    }
}
