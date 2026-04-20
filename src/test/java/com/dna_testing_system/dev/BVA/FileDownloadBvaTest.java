package com.dna_testing_system.dev.BVA;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.controller.FileDownloadController;
import com.dna_testing_system.dev.controller.NotificationController;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FileDownloadController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationController.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = true)
class FileDownloadBvaTest {

    @Autowired
    MockMvc mockMvc;

    private Path uploadDir;
    private Path sampleFile;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
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
        if (uploadDir == null || !Files.exists(uploadDir)) {
            return;
        }
        Files.walk(uploadDir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                        // best-effort cleanup for Windows
                    }
                });
    }

    // ======================= /files/download BVA =======================

    @ParameterizedTest
    @ValueSource(strings = {"sample.pdf"})
    void download_withoutAuth_returns401(String filename) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/download")
                        .param("filename", filename))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    @CsvSource({
            "'..',400",
            "'../evil.pdf',400",
            "'',404",
            "'missing.pdf',404",
            "'sample.pdf',200"
    })
    void download_filenameBoundary(String filename, int expectedStatus) throws Exception {
        var result = mockMvc.perform(MockMvcRequestBuilders.get("/files/download")
                        .param("filename", filename))
                .andExpect(status().is(expectedStatus));

        if (expectedStatus == 200) {
            result.andExpect(header().string("Content-Disposition", containsString("attachment")))
                    .andExpect(header().string("Content-Disposition", containsString("sample.pdf")));
        }
    }

    // ======================= /files/view BVA =======================

    @ParameterizedTest
    @ValueSource(strings = {"sample.pdf"})
    void view_withoutAuth_returns401(String filename) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/files/view")
                        .param("filename", filename))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @WithMockUser(username = "admin", roles = "ADMIN")
    @CsvSource({
            "'..',400",
            "'../evil.pdf',400",
            "'..\\\\evil.pdf',400",
            "'a/b.pdf',400",
            "'a\\\\b.pdf',400",
            "'',404",
            "'missing.pdf',404",
            "'sample.pdf',200"
    })
    void view_filenameBoundary(String filename, int expectedStatus) throws Exception {
        var result = mockMvc.perform(MockMvcRequestBuilders.get("/files/view")
                        .param("filename", filename))
                .andExpect(status().is(expectedStatus));

        if (expectedStatus == 200) {
            result.andExpect(header().string("Content-Disposition", containsString("inline")))
                    .andExpect(header().string("Content-Disposition", containsString("sample.pdf")))
                    .andExpect(header().exists("Content-Type"));
        }
    }
}