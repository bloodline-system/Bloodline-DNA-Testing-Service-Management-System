package com.dna_testing_system.dev.controller.feedback;

import com.dna_testing_system.dev.controller.UserFeedbackController;
import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.CustomerFeedbackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
class UserFeedbackControllerTest {

    @Autowired
    MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
    CustomerFeedbackService customerFeedbackService;

        @MockitoBean
    UserRepository userRepository;

    @Test
    void createFeedbackFormReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/feedback/{feedbackId}", "fb1")
                        .param("serviceId", "1")
                        .param("customerId", "ignored")
                        .param("serviceQualityRating", "5")
                        .param("staffBehaviorRating", "5")
                        .param("timelinessRating", "5"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("You must be logged in to submit feedback."));
    }

    @Test
    @WithMockUser(username = "alice")
    void createFeedbackFormReturns400WhenValidationFails() throws Exception {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(User.builder().id("u1").username("alice").passwordHash("x").build()));

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/feedback/{feedbackId}", "fb1")
                        .param("serviceId", "1")
                        .param("customerId", "temp")
                        .param("serviceQualityRating", "0")
                        .param("staffBehaviorRating", "5")
                        .param("timelinessRating", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Please fix the errors in the feedback form."));

        verify(customerFeedbackService, never()).createFeedback(any());
    }

    @Test
    @WithMockUser(username = "alice")
    void createFeedbackFormReturns200AndSetsCustomerId() throws Exception {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(User.builder().id("u1").username("alice").passwordHash("x").build()));

        when(customerFeedbackService.createFeedback(org.mockito.ArgumentMatchers.any(CreateFeedbackRequest.class)))
                .thenReturn(CustomerFeedbackResponse.builder().id(10L).customerName("Alice").build());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/feedback/{feedbackId}", "fb1")
                        .param("serviceId", "1")
                        .param("customerId", "temp")
                        .param("serviceQualityRating", "5")
                        .param("staffBehaviorRating", "4")
                        .param("timelinessRating", "3")
                        .param("feedbackTitle", "Nice")
                        .param("feedbackContent", "Good")
                        .param("responseRequired", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Feedback submitted successfully!"))
                .andExpect(jsonPath("$.data.id").value(10));

        ArgumentCaptor<CreateFeedbackRequest> captor = ArgumentCaptor.forClass(CreateFeedbackRequest.class);
        verify(customerFeedbackService).createFeedback(captor.capture());
        assertEquals("u1", captor.getValue().getCustomerId());
        assertEquals(1L, captor.getValue().getServiceId());
    }

    @Test
    void createFeedbackJsonReturns401WhenUnauthenticated() throws Exception {
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .serviceId(1L)
                .customerId("temp")
                .serviceQualityRating(5)
                .staffBehaviorRating(5)
                .timelinessRating(5)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feedback/{feedbackId}", "fb1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("You must be logged in to submit feedback."));
    }

    @Test
    @WithMockUser(username = "alice")
    void createFeedbackJsonReturns200WhenValid() throws Exception {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(User.builder().id("u1").username("alice").passwordHash("x").build()));
        when(customerFeedbackService.createFeedback(org.mockito.ArgumentMatchers.any(CreateFeedbackRequest.class)))
                .thenReturn(CustomerFeedbackResponse.builder().id(11L).build());

        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .serviceId(1L)
                .customerId("temp")
                .serviceQualityRating(5)
                .staffBehaviorRating(5)
                .timelinessRating(5)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feedback/{feedbackId}", "fb1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(11));

        ArgumentCaptor<CreateFeedbackRequest> captor = ArgumentCaptor.forClass(CreateFeedbackRequest.class);
        verify(customerFeedbackService).createFeedback(captor.capture());
        assertEquals("u1", captor.getValue().getCustomerId());
    }

    @Test
    void viewMyFeedbackReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/feedback"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("You must be logged in."));
    }

    @Test
    @WithMockUser(username = "alice")
    void viewMyFeedbackReturnsOk() throws Exception {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(User.builder().id("u1").username("alice").passwordHash("x").build()));
        when(customerFeedbackService.getFeedbackByCustomer("u1")).thenReturn(List.of(
                CustomerFeedbackResponse.builder().id(1L).build(),
                CustomerFeedbackResponse.builder().id(2L).build()
        ));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @WithMockUser(username = "alice")
    void getFeedbackByIdReturns404WhenServiceThrows() throws Exception {
        when(customerFeedbackService.getFeedbackById(99L)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/feedback/{feedbackId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Feedback not found"));
    }

    @Test
    @WithMockUser(username = "alice")
    void viewMyFeedback_userNotFound_returns500FromUnhandledEntityNotFound() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/feedback"))
                .andExpect(status().isInternalServerError());

        verify(customerFeedbackService, never()).getFeedbackByCustomer(anyString());
    }

    @Test
    @WithMockUser(username = "alice")
    void createFeedbackForm_userNotFound_returns500AndMessageContainsFailed() throws Exception {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/v1/feedback/{feedbackId}", "fb1")
                        .param("serviceId", "1")
                        .param("customerId", "temp")
                        .param("serviceQualityRating", "5")
                        .param("staffBehaviorRating", "5")
                        .param("timelinessRating", "5"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message", containsString("Failed to submit feedback")));
    }

    @Test
    @WithMockUser(username = "alice")
    void createFeedbackJson_serviceThrows_returns500() throws Exception {
        when(userRepository.findByUsername("alice"))
                .thenReturn(Optional.of(User.builder().id("u1").username("alice").passwordHash("x").build()));
        when(customerFeedbackService.createFeedback(org.mockito.ArgumentMatchers.any(CreateFeedbackRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .serviceId(1L)
                .customerId("temp")
                .serviceQualityRating(5)
                .staffBehaviorRating(5)
                .timelinessRating(5)
                .build();

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feedback/{feedbackId}", "fb1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message", containsString("Failed to submit feedback")));
    }

    @Test
    void getFeedbackById_returns200WhenFound() throws Exception {
        when(customerFeedbackService.getFeedbackById(1L))
                .thenReturn(CustomerFeedbackResponse.builder().id(1L).feedbackTitle("t").build());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/feedback/{feedbackId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get feedback successfully"))
                .andExpect(jsonPath("$.data.id").value(1));
    }
}

