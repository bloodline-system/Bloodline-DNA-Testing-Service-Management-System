package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.test_kit.TestKitController;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.staff.TestKitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * BVA (Boundary Value Analysis) – POST /api/v1/test-kits (CreateTestKit)
 *
 * Mỗi trường có ràng buộc số/độ dài được test đúng 5 điểm biên:
 *
 *   min−1  (invalid → 400)
 *   min    (valid   → 201)
 *   valid  (giá trị điển hình nằm giữa khoảng → 201)
 *   max    (valid   → 201)
 *   max+1  (invalid → 400)
 *
 * Các trường không có ràng buộc số/độ dài (kitType, sampleType, expiryDate nullable)
 * và các trường hợp null/optional không nằm trong phạm vi BVA này.
 *
 * Ràng buộc từ TestKitRequest:
 *   kitName         @NotBlank @Size(max=255)                         → độ dài [1..255]
 *   basePrice       @DecimalMin("0.0" inclusive) @Digits(integer=8)  → [0.00 .. 99999999.99]
 *   currentPrice    @DecimalMin("0.0" inclusive) @Digits(integer=8)  → [0.00 .. 99999999.99]
 *   quantityInStock @Min(0)                                          → [0 .. Integer.MAX_VALUE]
 *   kitDescription  @Size(max=1000) nullable                         → độ dài [0..1000]
 *   producedBy      @NotBlank @Size(max=255)                         → độ dài [1..255]
 *   expiryDate      @Future (strictly after today)                   → [today+1 .. ∞)
 */
@WebMvcTest(
        controllers = TestKitController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class TestKitBvaTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TestKitService testKitService;

    // =========================================================================
    // PHẦN 1 – kitName  |  @NotBlank  @Size(max=255)  →  độ dài [1 .. 255]
    //
    //   min−1 = 0  (blank)  → 400
    //   min   = 1           → 201
    //   valid = 128         → 201
    //   max   = 255         → 201
    //   max+1 = 256         → 400
    // =========================================================================

    /** BVA-KN-01: kitName length = 0  (min−1, blank) → 400 */
    @Test
    void createTestKit_kitName_length0_minMinus1_returns400() throws Exception {
        performPost(buildJson("", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-KN-02: kitName length = 1  (min) → 201 */
    @Test
    void createTestKit_kitName_length1_min_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("a", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-KN-03: kitName length = 128  (valid) → 201 */
    @Test
    void createTestKit_kitName_length128_valid_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("a".repeat(128), "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-KN-04: kitName length = 255  (max) → 201 */
    @Test
    void createTestKit_kitName_length255_max_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("a".repeat(255), "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-KN-05: kitName length = 256  (max+1) → 400 */
    @Test
    void createTestKit_kitName_length256_maxPlus1_returns400() throws Exception {
        performPost(buildJson("a".repeat(256), "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =========================================================================
    // PHẦN 2 – basePrice  |  @DecimalMin("0.0" inclusive)  @Digits(integer=8, fraction=2)
    //          → [0.00 .. 99999999.99]
    //
    //   min−1 = −0.01        → 400
    //   min   =  0.00        → 201
    //   valid =  500.00      → 201
    //   max   =  99999999.99  → 201
    //   max+1 =  99999999.999 (fraction vượt 2 chữ số) → 400
    // =========================================================================

    /** BVA-BP-01: basePrice = −0.01  (min−1) → 400 */
    @Test
    void createTestKit_basePrice_minMinus1_negative_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "-0.01", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-BP-02: basePrice = 0.00  (min) → 201 */
    @Test
    void createTestKit_basePrice_min_zero_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "0.00", "0.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-BP-03: basePrice = 500.00  (valid) → 201 */
    @Test
    void createTestKit_basePrice_valid_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "500.00", "450.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-BP-04: basePrice = 99999999.99  (max theo @Digits(integer=8, fraction=2)) → 201 */
    @Test
    void createTestKit_basePrice_max_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "99999999.99", "99999999.99", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-BP-05: basePrice = 99999999.999  (max+1, fraction vượt 2 chữ số) → 400 */
    @Test
    void createTestKit_basePrice_maxPlus1_fractionOverflow_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "99999999.999", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =========================================================================
    // PHẦN 3 – currentPrice  |  @DecimalMin("0.0" inclusive)  @Digits(integer=8, fraction=2)
    //          → [0.00 .. 99999999.99]
    //
    //   min−1 = −0.01        → 400
    //   min   =  0.00        → 201
    //   valid =  450.00      → 201
    //   max   =  99999999.99  → 201
    //   max+1 =  99999999.999 → 400
    // =========================================================================

    /** BVA-CP-01: currentPrice = −0.01  (min−1) → 400 */
    @Test
    void createTestKit_currentPrice_minMinus1_negative_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "-0.01", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-CP-02: currentPrice = 0.00  (min) → 201 */
    @Test
    void createTestKit_currentPrice_min_zero_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "0.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-CP-03: currentPrice = 450.00  (valid) → 201 */
    @Test
    void createTestKit_currentPrice_valid_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "500.00", "450.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-CP-04: currentPrice = 99999999.99  (max) → 201 */
    @Test
    void createTestKit_currentPrice_max_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "99999999.99", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-CP-05: currentPrice = 99999999.999  (max+1, fraction vượt 2 chữ số) → 400 */
    @Test
    void createTestKit_currentPrice_maxPlus1_fractionOverflow_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "99999999.999", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =========================================================================
    // PHẦN 4 – quantityInStock  |  @Min(0)  →  [0 .. Integer.MAX_VALUE]
    //
    //   min−1 = −1                   → 400
    //   min   =  0                   → 201
    //   valid =  500                 → 201
    //   max   =  Integer.MAX_VALUE   → 201
    //   max+1 =  2147483648 (vượt Integer) → 400
    // =========================================================================

    /** BVA-QT-01: quantityInStock = −1  (min−1) → 400 */
    @Test
    void createTestKit_quantity_minMinus1_negative_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "120.00", "-1",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-QT-02: quantityInStock = 0  (min) → 201 */
    @Test
    void createTestKit_quantity_min_zero_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "0",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-QT-03: quantityInStock = 500  (valid) → 201 */
    @Test
    void createTestKit_quantity_valid_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "500",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-QT-04: quantityInStock = 2147483647  (max = Integer.MAX_VALUE) → 201 */
    @Test
    void createTestKit_quantity_max_integerMaxValue_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00",
                String.valueOf(Integer.MAX_VALUE),
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-QT-05: quantityInStock = 2147483648  (max+1, vượt Integer) → 400 */
    @Test
    void createTestKit_quantity_maxPlus1_integerOverflow_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "120.00",
                String.valueOf((long) Integer.MAX_VALUE + 1),
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =========================================================================
    // PHẦN 5 – kitDescription  |  @Size(max=1000) nullable  →  độ dài [0 .. 1000]
    //
    //   min−1 = không áp dụng (độ dài chuỗi không thể < 0)
    //   min   = 0   (empty string) → 201
    //   valid = 500                → 201
    //   max   = 1000               → 201
    //   max+1 = 1001               → 400
    // =========================================================================

    /** BVA-KD-01: kitDescription length = 0  (min, empty string) → 201 */
    @Test
    void createTestKit_description_length0_min_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-KD-02: kitDescription length = 500  (valid) → 201 */
    @Test
    void createTestKit_description_length500_valid_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "a".repeat(500), LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-KD-03: kitDescription length = 1000  (max) → 201 */
    @Test
    void createTestKit_description_length1000_max_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "a".repeat(1000), LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-KD-04: kitDescription length = 1001  (max+1) → 400 */
    @Test
    void createTestKit_description_length1001_maxPlus1_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "a".repeat(1001), LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =========================================================================
    // PHẦN 6 – expiryDate  |  @Future (strictly after today)  →  [today+1 .. ∞)
    //
    //   min−1 = today − 1  (quá khứ)        → 400
    //   min   = today      (không hợp lệ với @Future strict) → 400
    //   valid = today + 30                   → 201
    //   max   = today + 5 năm (không giới hạn trên, lấy giá trị xa hợp lý) → 201
    //   max+1 = không áp dụng (không có giới hạn trên)
    // =========================================================================

    /** BVA-ED-01: expiryDate = today − 1  (min−1, quá khứ) → 400 */
    @Test
    void createTestKit_expiryDate_minMinus1_yesterday_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().minusDays(1), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-ED-02: expiryDate = today  (min, @Future không chấp nhận today) → 400 */
    @Test
    void createTestKit_expiryDate_min_today_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now(), "Producer A"))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-ED-03: expiryDate = today + 30  (valid) → 201 */
    @Test
    void createTestKit_expiryDate_valid_plus30days_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A"))
                .andExpect(status().isCreated());
    }

    /** BVA-ED-04: expiryDate = today + 5 năm  (max hợp lý, không có giới hạn trên) → 201 */
    @Test
    void createTestKit_expiryDate_max_farFuture_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusYears(5), "Producer A"))
                .andExpect(status().isCreated());
    }

    // =========================================================================
    // PHẦN 7 – producedBy  |  @NotBlank  @Size(max=255)  →  độ dài [1 .. 255]
    //
    //   min−1 = 0  (blank)  → 400
    //   min   = 1           → 201
    //   valid = 128         → 201
    //   max   = 255         → 201
    //   max+1 = 256         → 400
    // =========================================================================

    /** BVA-PB-01: producedBy length = 0  (min−1, blank) → 400 */
    @Test
    void createTestKit_producedBy_length0_minMinus1_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), ""))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-PB-02: producedBy length = 1  (min) → 201 */
    @Test
    void createTestKit_producedBy_length1_min_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "a"))
                .andExpect(status().isCreated());
    }

    /** BVA-PB-03: producedBy length = 128  (valid) → 201 */
    @Test
    void createTestKit_producedBy_length128_valid_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "a".repeat(128)))
                .andExpect(status().isCreated());
    }

    /** BVA-PB-04: producedBy length = 255  (max) → 201 */
    @Test
    void createTestKit_producedBy_length255_max_returns201() throws Exception {
        mockSuccess();
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "a".repeat(255)))
                .andExpect(status().isCreated());
    }

    /** BVA-PB-05: producedBy length = 256  (max+1) → 400 */
    @Test
    void createTestKit_producedBy_length256_maxPlus1_returns400() throws Exception {
        performPost(buildJson("Valid Kit", "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "a".repeat(256)))
                .andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private void mockSuccess() {
        doNothing().when(testKitService).CreateTestKit(any());
    }

    private org.springframework.test.web.servlet.ResultActions performPost(String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/test-kits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /**
     * Tạo JSON request với kitType=PATERNITY và sampleType=BLOOD cố định.
     * Chỉ trường đang được test BVA mới thay đổi giá trị.
     */
    private String buildJson(String kitName,
                             String basePrice, String currentPrice, String quantity,
                             String description, LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":\"%s\"," +
                "\"kitType\":\"PATERNITY\"," +
                "\"sampleType\":\"BLOOD\"," +
                "\"basePrice\":%s," +
                "\"currentPrice\":%s," +
                "\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\"," +
                "\"expiryDate\":\"%s\"," +
                "\"producedBy\":\"%s\"}",
                kitName, basePrice, currentPrice, quantity,
                description, expiryDate, producedBy);
    }
}