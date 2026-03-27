package com.dna_testing_system.dev.controller.files;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.FileDownloadController;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Bộ test cho FileDownloadController (tải xuống / xem file tài liệu)
// Mục tiêu:
// - Đảm bảo security: /files/** yêu cầu authenticated (401 nếu chưa đăng nhập)
// - Đảm bảo hành vi controller: 200 khi file tồn tại, 400 với filename không hợp lệ, 404 khi không tìm thấy file
// Lưu ý: Sử dụng @WebMvcTest + SecurityFilterChain riêng trong @TestConfiguration để không đụng tới cấu hình bảo mật toàn hệ thống.
@WebMvcTest(controllers = FileDownloadController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
})
@AutoConfigureMockMvc(addFilters = true)
class FileDownloadControllerTest {

    @Autowired
    MockMvc mockMvc;

    private Path uploadDir;
    private Path sampleFile;

    @TestConfiguration
    static class TestSecurityConfig {
        // Cấu hình Security đơn giản chỉ dùng cho test:
        // - Stateless
        // - Mọi request đều cần authenticated
        // - Chưa đăng nhập -> trả 401 (Unauthorized)
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    // Align expected behavior with app security: unauthenticated -> 401
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint((request, response, authException) -> response.setStatus(401)));
            return http.build();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        uploadDir = Path.of("uploads_information");
        Files.createDirectories(uploadDir);
        sampleFile = uploadDir.resolve("sample.pdf");
        Files.write(sampleFile, "%PDF-1.4 sample".getBytes());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (uploadDir == null || !Files.exists(uploadDir))
            return;
        // Dọn dẹp thư mục test (Windows hay bị khóa file nên cần xóa từ file con lên
        // cha)
        Files.walk(uploadDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                        // best-effort cleanup in tests
                    }
                });
    }

    // ======================= /files/download =======================

    // Chưa đăng nhập -> SecurityFilterChain chặn và trả về 401
    @Test
    void download_withoutAuth_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/download")
                .param("filename", "sample.pdf"))
                .andExpect(status().isUnauthorized());
    }

    // Đã đăng nhập (WithMockUser) + file tồn tại -> trả về 200 với header
    // attachment
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void download_withAuth_andExistingFile_returnsAttachment() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/download")
                .param("filename", "sample.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("sample.pdf")));
    }

    // filename chứa ".." -> controller trả về 400 (bảo vệ path traversal)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void download_withInvalidFilename_returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/download")
                .param("filename", "../evil.pdf"))
                .andExpect(status().isBadRequest());
    }

    // File không tồn tại trong thư mục uploads_information -> trả về 404
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void download_missingFile_returns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/download")
                .param("filename", "missing.pdf"))
                .andExpect(status().isNotFound());
    }

    // ======================= /files/view =======================

    // Chưa đăng nhập -> bị chặn bởi security, trả về 401
    @Test
    void view_withoutAuth_returns401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/view")
                .param("filename", "sample.pdf"))
                .andExpect(status().isUnauthorized());
    }

    // Đã đăng nhập + file tồn tại -> trả về 200, hiển thị inline với Content-Type
    // phù hợp
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void view_withAuth_andExistingFile_returnsInlineAndContentType() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/view")
                .param("filename", "sample.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(header().string("Content-Disposition", containsString("sample.pdf")))
                .andExpect(header().exists("Content-Type"));
    }

    // filename chứa ".." hoặc ký tự đường dẫn -> controller trả về 400
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void view_withInvalidFilename_returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/view")
                .param("filename", "..\\evil.pdf"))
                .andExpect(status().isBadRequest());
    }

    // File không tồn tại -> 404
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void view_missingFile_returns404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/view")
                .param("filename", "missing.pdf"))
                .andExpect(status().isNotFound());
    }
}
