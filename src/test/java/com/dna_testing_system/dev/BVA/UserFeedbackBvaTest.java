package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.UserFeedbackController;
import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.CustomerFeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserFeedbackController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class UserFeedbackBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CustomerFeedbackService customerFeedbackService;

    @MockitoBean
    UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(User.builder().id("u1").username("alice").passwordHash("x").build()));
        when(customerFeedbackService.createFeedback(any(CreateFeedbackRequest.class)))
                .thenReturn(CustomerFeedbackResponse.builder().id(1L).build());
    }

    @ParameterizedTest
    @WithMockUser(username = "alice")
    @CsvSource({
            "serviceQualityRating,0,400",
            "serviceQualityRating,1,200",
            "serviceQualityRating,5,200",
            "serviceQualityRating,6,400",
            "staffBehaviorRating,0,400",
            "staffBehaviorRating,1,200",
            "staffBehaviorRating,5,200",
            "staffBehaviorRating,6,400",
            "timelinessRating,0,400",
            "timelinessRating,1,200",
            "timelinessRating,5,200",
            "timelinessRating,6,400"
    })
    void createFeedbackJson_ratingBva(String field, int value, int expectedStatus) throws Exception {
        CreateFeedbackRequest request = validRequest();

        if ("serviceQualityRating".equals(field)) {
            request.setServiceQualityRating(value);
        } else if ("staffBehaviorRating".equals(field)) {
            request.setStaffBehaviorRating(value);
        } else {
            request.setTimelinessRating(value);
        }

        var result = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feedback/{feedbackId}", "fb1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().is(expectedStatus));

        if (expectedStatus == 200) {
            result.andExpect(jsonPath("$.code").value(200));
        } else {
            verify(customerFeedbackService, never()).createFeedback(any(CreateFeedbackRequest.class));
        }
    }

    @ParameterizedTest
    @WithMockUser(username = "alice")
    @ValueSource(ints = {255, 256})
    void createFeedbackJson_titleBoundary(int length) throws Exception {
        CreateFeedbackRequest request = validRequest();
        request.setFeedbackTitle("t".repeat(length));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feedback/{feedbackId}", "fb1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().is(length == 255 ? 200 : 400));

        if (length == 256) {
            verify(customerFeedbackService, never()).createFeedback(any(CreateFeedbackRequest.class));
        }
    }

    @ParameterizedTest
    @WithMockUser(username = "alice")
    @ValueSource(strings = {"missingServiceId", "missingCustomerId"})
    void createFeedbackJson_requiredFieldsBoundary(String scenario) throws Exception {
        CreateFeedbackRequest request = validRequest();
        if ("missingServiceId".equals(scenario)) {
            request.setServiceId(null);
        } else {
            request.setCustomerId(null);
        }

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feedback/{feedbackId}", "fb1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isBadRequest());

        verify(customerFeedbackService, never()).createFeedback(any(CreateFeedbackRequest.class));
    }

    @ParameterizedTest
    @WithMockUser(username = "alice")
    @CsvSource({
            "1,200",
            "5,200",
            "0,400",
            "6,400"
    })
    void createFeedbackForm_ratingBoundary(int serviceQualityRating, int expectedStatus) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/feedback/{feedbackId}", "fb1")
                        .param("serviceId", "1")
                        .param("customerId", "temp")
                        .param("serviceQualityRating", String.valueOf(serviceQualityRating))
                        .param("staffBehaviorRating", "3")
                        .param("timelinessRating", "3")
                        .param("feedbackTitle", "Good"))
                .andExpect(status().is(expectedStatus));

        if (expectedStatus == 400) {
            verify(customerFeedbackService, never()).createFeedback(any(CreateFeedbackRequest.class));
        }
    }

    private CreateFeedbackRequest validRequest() {
        return CreateFeedbackRequest.builder()
                .serviceId(1L)
                .customerId("temp")
                .serviceQualityRating(3)
                .staffBehaviorRating(3)
                .timelinessRating(3)
                .feedbackTitle("Good")
                .feedbackContent("Works well")
                .responseRequired(false)
                .build();
    }
}
