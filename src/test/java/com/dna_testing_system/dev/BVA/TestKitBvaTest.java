package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.controller.test_kit.TestKitController;
import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
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

    // =====================================================================
    // PHẦN 1 – kitName  (@NotBlank, @Size(max=255))
    // Biên: 0(invalid) | 1(min-valid) | 2(min+1) | 254(max-1) | 255(max-valid) | 256(invalid)
    // =====================================================================

    /** BVA-KN-01: kitName = "" (length=0) → @NotBlank vi phạm → 400 */
    @Test
    void createTestKit_kitName_length0_blank_returns400() throws Exception {
        String json = buildJson("", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-KN-02: kitName = null → @NotBlank vi phạm → 400 */
    @Test
    void createTestKit_kitName_null_returns400() throws Exception {
        String json = buildJsonWithNull("kitName", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-KN-03: kitName length=1 (biên dưới hợp lệ) → 201 */
    @Test
    void createTestKit_kitName_length1_minValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("a", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KN-04: kitName length=2 (min+1) → 201 */
    @Test
    void createTestKit_kitName_length2_minPlusOne_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("ab", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KN-05: kitName length=254 (max-1) → 201 */
    @Test
    void createTestKit_kitName_length254_maxMinusOne_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("a".repeat(254), "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KN-06: kitName length=255 (biên trên hợp lệ) → 201 */
    @Test
    void createTestKit_kitName_length255_maxValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("a".repeat(255), "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KN-07: kitName length=256 (max+1) → @Size vi phạm → 400 */
    @Test
    void createTestKit_kitName_length256_maxPlusOne_returns400() throws Exception {
        String json = buildJson("a".repeat(256), "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =====================================================================
    // PHẦN 2 – kitType  (@NotNull, enum KitType)
    // Biên: null(invalid) | giá trị hợp lệ | giá trị không thuộc enum(invalid)
    // =====================================================================

    /** BVA-KT-01: kitType = null → @NotNull vi phạm → 400 */
    @Test
    void createTestKit_kitType_null_returns400() throws Exception {
        String json = buildJsonNullKitType("BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-KT-02: kitType = "PATERNITY" (giá trị hợp lệ đầu tiên trong enum) → 201 */
    @Test
    void createTestKit_kitType_PATERNITY_valid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KT-03: kitType = "OTHER" (giá trị hợp lệ cuối cùng trong enum) → 201 */
    @Test
    void createTestKit_kitType_OTHER_lastEnumValue_valid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "OTHER", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KT-04: kitType = "INVALID_TYPE" (không thuộc enum) → 500 (deserialization error) */
    @Test
    void createTestKit_kitType_invalidEnumValue_returns500() throws Exception {
        String json = buildJson("Valid Kit", "INVALID_TYPE", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isInternalServerError());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =====================================================================
    // PHẦN 3 – sampleType  (@NotNull, enum SampleType)
    // Biên: null(invalid) | giá trị hợp lệ | giá trị không thuộc enum(invalid)
    // =====================================================================

    /** BVA-ST-01: sampleType = null → @NotNull vi phạm → 400 */
    @Test
    void createTestKit_sampleType_null_returns400() throws Exception {
        String json = buildJsonNullSampleType("PATERNITY",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-ST-02: sampleType = "BLOOD" (giá trị hợp lệ đầu tiên) → 201 */
    @Test
    void createTestKit_sampleType_BLOOD_valid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-ST-03: sampleType = "OTHER" (giá trị hợp lệ cuối cùng trong enum) → 201 */
    @Test
    void createTestKit_sampleType_OTHER_lastEnumValue_valid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "OTHER",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-ST-04: sampleType = "PLASMA" (không thuộc enum SampleType) → 500 (deserialization error) */
    @Test
    void createTestKit_sampleType_invalidEnumValue_returns500() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "PLASMA",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isInternalServerError());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =====================================================================
    // PHẦN 4 – basePrice  (@NotNull, @DecimalMin("0.0" inclusive), @Digits(integer=8, fraction=2))
    // Biên: null | -0.01(invalid) | 0.00(min) | 0.01(min+1) | 99999999.99(max) | 999999999.99(integer>8, invalid)
    // =====================================================================

    /** BVA-BP-01: basePrice = null → @NotNull vi phạm → 400 */
    @Test
    void createTestKit_basePrice_null_returns400() throws Exception {
        String json = buildJsonNullBasePrice("Valid Kit", "PATERNITY", "BLOOD",
                "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-BP-02: basePrice = -0.01 (dưới min) → @DecimalMin vi phạm → 400 */
    @Test
    void createTestKit_basePrice_negativeBelowMin_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "-0.01", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-BP-03: basePrice = 0.00 (biên dưới hợp lệ, inclusive) → 201 */
    @Test
    void createTestKit_basePrice_zero_minValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "0.00", "0.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-BP-04: basePrice = 0.01 (min+1 đơn vị) → 201 */
    @Test
    void createTestKit_basePrice_zeroPointOne_minPlusUnit_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "0.01", "0.01", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-BP-05: basePrice = 99999999.99 (max hợp lệ theo @Digits(integer=8, fraction=2)) → 201 */
    @Test
    void createTestKit_basePrice_maxValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "99999999.99", "99999999.99", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-BP-06: basePrice = 999999999.99 (integer part > 8 digits) → @Digits vi phạm → 400 */
    @Test
    void createTestKit_basePrice_integerPartExceedsDigits_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "999999999.99", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-BP-07: basePrice = 100.001 (fraction > 2 digits) → @Digits vi phạm → 400 */
    @Test
    void createTestKit_basePrice_fractionExceedsDigits_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.001", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =====================================================================
    // PHẦN 5 – currentPrice  (@NotNull, @DecimalMin("0.0" inclusive), @Digits(integer=8, fraction=2))
    // Biên: null | -0.01(invalid) | 0.00(min) | 0.01(min+1) | 99999999.99(max) | overflow(invalid)
    // =====================================================================

    /** BVA-CP-01: currentPrice = null → @NotNull vi phạm → 400 */
    @Test
    void createTestKit_currentPrice_null_returns400() throws Exception {
        String json = buildJsonNullCurrentPrice("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-CP-02: currentPrice = -0.01 (dưới min) → @DecimalMin vi phạm → 400 */
    @Test
    void createTestKit_currentPrice_negativeBelowMin_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "-0.01", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-CP-03: currentPrice = 0.00 (biên dưới hợp lệ, inclusive) → 201 */
    @Test
    void createTestKit_currentPrice_zero_minValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "0.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-CP-04: currentPrice = 0.01 (min+1 đơn vị) → 201 */
    @Test
    void createTestKit_currentPrice_zeroPointOne_minPlusUnit_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "0.01", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-CP-05: currentPrice = 99999999.99 (max hợp lệ theo @Digits(8,2)) → 201 */
    @Test
    void createTestKit_currentPrice_maxValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "99999999.99", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-CP-06: currentPrice = 999999999.99 (integer > 8 digits) → @Digits vi phạm → 400 */
    @Test
    void createTestKit_currentPrice_integerPartExceedsDigits_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "999999999.99", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-CP-07: currentPrice = 100.001 (fraction > 2 digits) → @Digits vi phạm → 400 */
    @Test
    void createTestKit_currentPrice_fractionExceedsDigits_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "100.001", "50",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =====================================================================
    // PHẦN 6 – quantityInStock  (@NotNull, @Min(0))
    // Biên: null | -1(invalid) | 0(min) | 1(min+1) | Integer.MAX_VALUE(max hợp lệ)
    // =====================================================================

    /** BVA-QT-01: quantityInStock = null → @NotNull vi phạm → 400 */
    @Test
    void createTestKit_quantity_null_returns400() throws Exception {
        String json = buildJsonNullQuantity("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-QT-02: quantityInStock = -1 (dưới min) → @Min vi phạm → 400 */
    @Test
    void createTestKit_quantity_negativeBelowMin_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "-1",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-QT-03: quantityInStock = 0 (biên dưới hợp lệ) → 201 */
    @Test
    void createTestKit_quantity_zero_minValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "0",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-QT-04: quantityInStock = 1 (min+1) → 201 */
    @Test
    void createTestKit_quantity_one_minPlusOne_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "1",
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-QT-05: quantityInStock = Integer.MAX_VALUE (giá trị int lớn nhất) → 201 */
    @Test
    void createTestKit_quantity_integerMaxValue_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", String.valueOf(Integer.MAX_VALUE),
                "Valid description", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    // =====================================================================
    // PHẦN 7 – kitDescription  (@Size(max=1000), field optional/nullable)
    // Biên: null(valid) | 0(valid) | 1(min+1) | 999(max-1) | 1000(max) | 1001(invalid)
    // =====================================================================

    /** BVA-KD-01: kitDescription = null (field optional, null hợp lệ) → 201 */
    @Test
    void createTestKit_description_null_optional_returns201() throws Exception {
        mockSuccess();
        String json = buildJsonNullDescription("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KD-02: kitDescription = "" (length=0, empty string) → 201 */
    @Test
    void createTestKit_description_length0_empty_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KD-03: kitDescription length=1 (min+1) → 201 */
    @Test
    void createTestKit_description_length1_minPlusOne_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "a", LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KD-04: kitDescription length=999 (max-1) → 201 */
    @Test
    void createTestKit_description_length999_maxMinusOne_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "a".repeat(999), LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KD-05: kitDescription length=1000 (biên trên hợp lệ) → 201 */
    @Test
    void createTestKit_description_length1000_maxValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "a".repeat(1000), LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-KD-06: kitDescription length=1001 (max+1) → @Size vi phạm → 400 */
    @Test
    void createTestKit_description_length1001_maxPlusOne_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "a".repeat(1001), LocalDate.now().plusDays(30), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =====================================================================
    // PHẦN 8 – expiryDate  (@Future – strictly after today, field nullable)
    // Biên: null(valid) | today-1(invalid) | today(invalid) | today+1(min-valid) | far-future(valid)
    // =====================================================================

    /** BVA-ED-01: expiryDate = null (field nullable, không có @NotNull) → 201 */
    @Test
    void createTestKit_expiryDate_null_optional_returns201() throws Exception {
        mockSuccess();
        String json = buildJsonNullExpiryDate("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-ED-02: expiryDate = today - 1 ngày (quá khứ) → @Future vi phạm → 400 */
    @Test
    void createTestKit_expiryDate_yesterday_pastDate_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().minusDays(1), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-ED-03: expiryDate = today (hiện tại) → @Future yêu cầu strictly future → 400 */
    @Test
    void createTestKit_expiryDate_today_notFuture_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now(), "Producer A");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-ED-04: expiryDate = today + 1 (biên dưới hợp lệ của @Future) → 201 */
    @Test
    void createTestKit_expiryDate_tomorrow_minValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(1), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-ED-05: expiryDate = today + 5 năm (ngày tương lai xa) → 201 */
    @Test
    void createTestKit_expiryDate_farFuture_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusYears(5), "Producer A");

        performPost(json).andExpect(status().isCreated());
    }

    // =====================================================================
    // PHẦN 9 – producedBy  (@NotBlank, @Size(max=255))
    // Biên: null | 0(invalid) | 1(min) | 2(min+1) | 254(max-1) | 255(max) | 256(invalid)
    // =====================================================================

    /** BVA-PB-01: producedBy = null → @NotBlank vi phạm → 400 */
    @Test
    void createTestKit_producedBy_null_returns400() throws Exception {
        String json = buildJsonNullProducedBy("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30));

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-PB-02: producedBy = "" (length=0) → @NotBlank vi phạm → 400 */
    @Test
    void createTestKit_producedBy_length0_blank_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "");

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    /** BVA-PB-03: producedBy length=1 (biên dưới hợp lệ) → 201 */
    @Test
    void createTestKit_producedBy_length1_minValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "a");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-PB-04: producedBy length=2 (min+1) → 201 */
    @Test
    void createTestKit_producedBy_length2_minPlusOne_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "ab");

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-PB-05: producedBy length=254 (max-1) → 201 */
    @Test
    void createTestKit_producedBy_length254_maxMinusOne_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "a".repeat(254));

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-PB-06: producedBy length=255 (biên trên hợp lệ) → 201 */
    @Test
    void createTestKit_producedBy_length255_maxValid_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "a".repeat(255));

        performPost(json).andExpect(status().isCreated());
    }

    /** BVA-PB-07: producedBy length=256 (max+1) → @Size vi phạm → 400 */
    @Test
    void createTestKit_producedBy_length256_maxPlusOne_returns400() throws Exception {
        String json = buildJson("Valid Kit", "PATERNITY", "BLOOD",
                "100.00", "120.00", "50",
                "Valid description", LocalDate.now().plusDays(30), "a".repeat(256));

        performPost(json).andExpect(status().isBadRequest());
        verify(testKitService, never()).CreateTestKit(any());
    }

    // =====================================================================
    // PHẦN 10 – Happy path: tất cả trường hợp lệ cùng lúc
    // =====================================================================

    /** BVA-HP-01: tất cả trường hợp lệ với giá trị điển hình → 201 */
    @Test
    void createTestKit_allFieldsValid_typical_returns201() throws Exception {
        mockSuccess();
        String json = buildJson("Standard DNA Kit", "PATERNITY", "BLOOD",
                "500.00", "450.00", "100",
                "Standard paternity test kit", LocalDate.now().plusDays(180), "BioLab Corp");

        performPost(json).andExpect(status().isCreated());
        verify(testKitService, times(1)).CreateTestKit(any());
    }

    /** BVA-HP-02: tất cả trường hợp lệ với giá trị tại các biên (min/max) → 201 */
    @Test
    void createTestKit_allFieldsAtBoundary_returns201() throws Exception {
        mockSuccess();
        // kitName=1, basePrice=0, currentPrice=0, qty=0, description="", producedBy=1, expiry=+1day
        String json = buildJson("a", "OTHER", "OTHER",
                "0.00", "0.00", "0",
                "", LocalDate.now().plusDays(1), "b");

        performPost(json).andExpect(status().isCreated());
        verify(testKitService, times(1)).CreateTestKit(any());
    }

    // =====================================================================
    // Helper methods
    // =====================================================================

    private void mockSuccess() {
        doNothing().when(testKitService).CreateTestKit(any());
    }

    private org.springframework.test.web.servlet.ResultActions performPost(String json) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/test-kits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    /** JSON đầy đủ tất cả trường, không có null field */
    private String buildJson(String kitName, String kitType, String sampleType,
                             String basePrice, String currentPrice, String quantity,
                             String description, LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":\"%s\",\"kitType\":\"%s\",\"sampleType\":\"%s\"," +
                "\"basePrice\":%s,\"currentPrice\":%s,\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\",\"expiryDate\":\"%s\",\"producedBy\":\"%s\"}",
                kitName, kitType, sampleType,
                basePrice, currentPrice, quantity,
                description, expiryDate, producedBy);
    }

    /** kitName = null (omit field) */
    private String buildJsonWithNull(String nullField, String kitType, String sampleType,
                                     String basePrice, String currentPrice, String quantity,
                                     String description, LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":null,\"kitType\":\"%s\",\"sampleType\":\"%s\"," +
                "\"basePrice\":%s,\"currentPrice\":%s,\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\",\"expiryDate\":\"%s\",\"producedBy\":\"%s\"}",
                kitType, sampleType, basePrice, currentPrice, quantity,
                description, expiryDate, producedBy);
    }

    /** kitType = null */
    private String buildJsonNullKitType(String sampleType,
                                        String basePrice, String currentPrice, String quantity,
                                        String description, LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":\"Valid Kit\",\"kitType\":null,\"sampleType\":\"%s\"," +
                "\"basePrice\":%s,\"currentPrice\":%s,\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\",\"expiryDate\":\"%s\",\"producedBy\":\"%s\"}",
                sampleType, basePrice, currentPrice, quantity, description, expiryDate, producedBy);
    }

    /** sampleType = null */
    private String buildJsonNullSampleType(String kitType,
                                           String basePrice, String currentPrice, String quantity,
                                           String description, LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":\"Valid Kit\",\"kitType\":\"%s\",\"sampleType\":null," +
                "\"basePrice\":%s,\"currentPrice\":%s,\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\",\"expiryDate\":\"%s\",\"producedBy\":\"%s\"}",
                kitType, basePrice, currentPrice, quantity, description, expiryDate, producedBy);
    }

    /** basePrice = null */
    private String buildJsonNullBasePrice(String kitName, String kitType, String sampleType,
                                          String currentPrice, String quantity,
                                          String description, LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":\"%s\",\"kitType\":\"%s\",\"sampleType\":\"%s\"," +
                "\"basePrice\":null,\"currentPrice\":%s,\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\",\"expiryDate\":\"%s\",\"producedBy\":\"%s\"}",
                kitName, kitType, sampleType, currentPrice, quantity,
                description, expiryDate, producedBy);
    }

    /** currentPrice = null */
    private String buildJsonNullCurrentPrice(String kitName, String kitType, String sampleType,
                                             String basePrice, String quantity,
                                             String description, LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":\"%s\",\"kitType\":\"%s\",\"sampleType\":\"%s\"," +
                "\"basePrice\":%s,\"currentPrice\":null,\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\",\"expiryDate\":\"%s\",\"producedBy\":\"%s\"}",
                kitName, kitType, sampleType, basePrice, quantity,
                description, expiryDate, producedBy);
    }

    /** quantityInStock = null */
    private String buildJsonNullQuantity(String kitName, String kitType, String sampleType,
                                         String basePrice, String currentPrice,
                                         String description, LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":\"%s\",\"kitType\":\"%s\",\"sampleType\":\"%s\"," +
                "\"basePrice\":%s,\"currentPrice\":%s,\"quantityInStock\":null," +
                "\"kitDescription\":\"%s\",\"expiryDate\":\"%s\",\"producedBy\":\"%s\"}",
                kitName, kitType, sampleType, basePrice, currentPrice,
                description, expiryDate, producedBy);
    }

    /** kitDescription = null (omit field entirely) */
    private String buildJsonNullDescription(String kitName, String kitType, String sampleType,
                                            String basePrice, String currentPrice, String quantity,
                                            LocalDate expiryDate, String producedBy) {
        return String.format(
                "{\"kitName\":\"%s\",\"kitType\":\"%s\",\"sampleType\":\"%s\"," +
                "\"basePrice\":%s,\"currentPrice\":%s,\"quantityInStock\":%s," +
                "\"expiryDate\":\"%s\",\"producedBy\":\"%s\"}",
                kitName, kitType, sampleType, basePrice, currentPrice, quantity,
                expiryDate, producedBy);
    }

    /** expiryDate = null (omit field) */
    private String buildJsonNullExpiryDate(String kitName, String kitType, String sampleType,
                                           String basePrice, String currentPrice, String quantity,
                                           String description, String producedBy) {
        return String.format(
                "{\"kitName\":\"%s\",\"kitType\":\"%s\",\"sampleType\":\"%s\"," +
                "\"basePrice\":%s,\"currentPrice\":%s,\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\",\"producedBy\":\"%s\"}",
                kitName, kitType, sampleType, basePrice, currentPrice, quantity,
                description, producedBy);
    }

    /** producedBy = null */
    private String buildJsonNullProducedBy(String kitName, String kitType, String sampleType,
                                           String basePrice, String currentPrice, String quantity,
                                           String description, LocalDate expiryDate) {
        return String.format(
                "{\"kitName\":\"%s\",\"kitType\":\"%s\",\"sampleType\":\"%s\"," +
                "\"basePrice\":%s,\"currentPrice\":%s,\"quantityInStock\":%s," +
                "\"kitDescription\":\"%s\",\"expiryDate\":\"%s\",\"producedBy\":null}",
                kitName, kitType, sampleType, basePrice, currentPrice, quantity,
                description, expiryDate);
    }
}