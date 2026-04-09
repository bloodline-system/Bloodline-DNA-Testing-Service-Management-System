package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.api.ApiAdminFeedbackController;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.CustomerFeedbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ApiAdminFeedbackFilterBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CustomerFeedbackService customerFeedbackService;

    @MockitoBean
    UserRepository userRepository;

    // =====================================================================
    // PHAN 1 - responseStatus filter boundary
    // Bien: all, responded, unresponded, invalid status
    // =====================================================================

    /** BVA-AF-RS-01..04: responseStatus theo cac moc bien logic */
    @ParameterizedTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    @CsvSource({
            "all,2",
            "responded,1",
            "unresponded,1",
            "invalid,0"
    })
    void getAllFeedbacks_responseStatusBoundary(String responseStatus, int expectedSize) throws Exception {
        when(customerFeedbackService.getAllFeedbacks(0, 20, ""))
                .thenReturn(new PageImpl<>(seedFeedback(), PageRequest.of(0, 20), 2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/feedback")
                        .param("responseStatus", responseStatus))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.feedbackList.length()").value(expectedSize));
    }

    // =====================================================================
    // PHAN 2 - customerName/serviceName contains filter
    // Bien: exact/partial/no-match
    // =====================================================================

    /** BVA-AF-CN-01: customerName partial match -> 1 */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllFeedbacks_customerNamePartialMatch_returnsOne() throws Exception {
        when(customerFeedbackService.getAllFeedbacks(0, 20, ""))
                .thenReturn(new PageImpl<>(seedFeedback(), PageRequest.of(0, 20), 2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/feedback")
                        .param("customerName", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackList.length()").value(1))
                .andExpect(jsonPath("$.data.feedbackList[0].customerName").value("Alice"));
    }

    /** BVA-AF-SN-01: serviceName no-match -> 0 */
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAllFeedbacks_serviceNameNoMatch_returnsZero() throws Exception {
        when(customerFeedbackService.getAllFeedbacks(0, 20, ""))
                .thenReturn(new PageImpl<>(seedFeedback(), PageRequest.of(0, 20), 2));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/feedback")
                        .param("serviceName", "XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackList.length()").value(0));
    }

    private List<CustomerFeedbackResponse> seedFeedback() {
        CustomerFeedbackResponse responded = CustomerFeedbackResponse.builder()
                .id(1L)
                .customerName("Alice")
                .serviceName("DNA Home")
                .responseRequired(true)
                .respondedAt(LocalDateTime.now())
                .build();

        CustomerFeedbackResponse unresponded = CustomerFeedbackResponse.builder()
                .id(2L)
                .customerName("Bob")
                .serviceName("DNA Lab")
                .responseRequired(true)
                .respondedAt(null)
                .build();

        return List.of(responded, unresponded);
    }
}
