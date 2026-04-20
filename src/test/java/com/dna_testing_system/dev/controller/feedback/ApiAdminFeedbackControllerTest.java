package com.dna_testing_system.dev.controller.feedback;

import com.dna_testing_system.dev.controller.api.ApiAdminFeedbackController;
import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.dto.request.RespondFeedbackRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ApiAdminFeedbackController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ApiAdminFeedbackControllerTest {

    @Autowired
    MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
    CustomerFeedbackService customerFeedbackService;

        @MockitoBean
    UserRepository userRepository;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllFeedbacksFiltersByResponseStatus() throws Exception {
        CustomerFeedbackResponse responded = CustomerFeedbackResponse.builder()
                .id(1L)
                .customerName("Alice")
                .serviceName("DNA")
                .responseRequired(true)
                .respondedAt(LocalDateTime.now())
                .overallRating(4.0f)
                .serviceQualityRating(4)
                .staffBehaviorRating(4)
                .timelinessRating(4)
                .build();

        CustomerFeedbackResponse unresponded = CustomerFeedbackResponse.builder()
                .id(2L)
                .customerName("Bob")
                .serviceName("DNA")
                .responseRequired(true)
                .respondedAt(null)
                .overallRating(5.0f)
                .serviceQualityRating(5)
                .staffBehaviorRating(5)
                .timelinessRating(5)
                .build();

        Page<CustomerFeedbackResponse> page = new PageImpl<>(
                List.of(responded, unresponded),
                PageRequest.of(0, 20),
                2
        );

        when(customerFeedbackService.getAllFeedbacks(0, 20, "")).thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/feedback")
                        .param("responseStatus", "responded"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.feedbackList", hasSize(1)))
                .andExpect(jsonPath("$.data.feedbackList[0].id").value(1))
                .andExpect(jsonPath("$.data.feedbackStats.totalFeedback").value(2))
                .andExpect(jsonPath("$.data.feedbackStats.respondedCount").value(1))
                .andExpect(jsonPath("$.data.feedbackStats.pendingResponseCount").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getFeedbackByIdReturns404WhenNull() throws Exception {
        when(customerFeedbackService.getFeedbackById(10L)).thenReturn(null);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/feedback/{id}", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Feedback not found"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void respondToFeedbackReturns400WhenResponseContentBlank() throws Exception {
        RespondFeedbackRequest req = RespondFeedbackRequest.builder().responseContent(" ").build();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/admin/feedback/{id}/respond", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("responseContent không được để trống"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void respondToFeedbackReturns401WhenUserNotFound() throws Exception {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        RespondFeedbackRequest req = RespondFeedbackRequest.builder().responseContent("Thanks").build();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/admin/feedback/{id}/respond", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void respondToFeedbackSetsRespondByUserIdAndReturnsOk() throws Exception {
        when(userRepository.findByUsername("admin"))
                .thenReturn(Optional.of(User.builder().id("admin-id").username("admin").passwordHash("x").build()));
        when(customerFeedbackService.respondToFeedback(eq(1L), org.mockito.ArgumentMatchers.any(RespondFeedbackRequest.class)))
                .thenReturn(CustomerFeedbackResponse.builder().id(1L).responseContent("Thanks").build());

        RespondFeedbackRequest req = RespondFeedbackRequest.builder().responseContent("Thanks").build();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/admin/feedback/{id}/respond", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));

        ArgumentCaptor<RespondFeedbackRequest> captor = ArgumentCaptor.forClass(RespondFeedbackRequest.class);
        verify(customerFeedbackService).respondToFeedback(eq(1L), captor.capture());
        assertEquals("admin-id", captor.getValue().getRespondByUserId());
        assertEquals("Thanks", captor.getValue().getResponseContent());
    }
}
