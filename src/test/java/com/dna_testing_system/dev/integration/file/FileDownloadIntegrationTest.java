package com.dna_testing_system.dev.integration.file;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.dna_testing_system.dev.dto.request.auth.AuthenticationRequestDTO;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.entity.Role;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.entity.UserRole;
import com.dna_testing_system.dev.integration.common.AbstractIntegrationTest;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.SignUpRepository;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for document download endpoints (3.1.9).
 */
@DisplayName("File download integration (3.1.9)")
class FileDownloadIntegrationTest extends AbstractIntegrationTest {

    private static final String CUSTOMER_USER = "it319customer";
    private static final String CUSTOMER_PASS = "ItCust123!";

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private SignUpRepository signUpRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private Path uploadDir;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
        signUpRepository.deleteAll();
        try {
            flushRedis();
        } catch (Exception ignored) {
        }

        Role roleCustomer = roleRepository.findByRoleName("CUSTOMER").orElseThrow();
        User user = User.builder()
                .username(CUSTOMER_USER)
                .passwordHash(passwordEncoder.encode(CUSTOMER_PASS))
                .isActive(true)
                .userRoles(new HashSet<>())
                .build();
        user = userRepository.save(user);
        UserRole ur = UserRole.builder().user(user).role(roleCustomer).isActive(true).build();
        userRoleRepository.save(ur);
        user.getUserRoles().add(ur);
        userRepository.save(user);
        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName("IT")
                .lastName("Customer")
                .email("it319@example.com")
                .phoneNumber("0900000002")
                .build();
        userProfileRepository.save(profile);
        user.setProfile(profile);
        userRepository.save(user);

        uploadDir = Path.of("uploads_information");
        Files.createDirectories(uploadDir);
        Files.writeString(uploadDir.resolve("dna-result.pdf"), "%PDF-1.4 test");
        Files.writeString(uploadDir.resolve("invoice.pdf"), "%PDF-1.4 invoice");
        Files.writeString(uploadDir.resolve("home-sampling-guide.pdf"), "%PDF-1.4 guide");
        Files.writeString(uploadDir.resolve("registration-form.pdf"), "%PDF-1.4 form");
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
                    }
                });
    }

    private String loginAccessToken() throws Exception {
        AuthenticationRequestDTO login = AuthenticationRequestDTO.builder()
                .username(CUSTOMER_USER)
                .password(CUSTOMER_PASS)
                .build();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        ApiResponse<AuthTokensResponseDTO> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthTokensResponseDTO.class));
        assertThat(body.getData()).isNotNull();
        return body.getData().getAccessToken();
    }

    @Test
    @DisplayName("Unauthenticated download returns 401")
    void download_withoutAuth_unauthorized() throws Exception {
        mockMvc.perform(get("/files/download").param("filename", "dna-result.pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated view returns 401")
    void view_withoutAuth_unauthorized() throws Exception {
        mockMvc.perform(get("/files/view").param("filename", "dna-result.pdf"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "download {0} with auth returns 200")
    @ValueSource(strings = {"dna-result.pdf", "invoice.pdf", "home-sampling-guide.pdf", "registration-form.pdf"})
    @DisplayName("Authenticated user can download representative document filenames")
    void download_withAuth_ok(String filename) throws Exception {
        String token = loginAccessToken();
        mockMvc.perform(get("/files/download")
                        .param("filename", filename)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")));
    }

    @Test
    @DisplayName("Path traversal in filename returns 400")
    void download_traversal_badRequest() throws Exception {
        String token = loginAccessToken();
        mockMvc.perform(get("/files/download")
                        .param("filename", "../evil.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Missing file returns 404")
    void download_missing_notFound() throws Exception {
        String token = loginAccessToken();
        mockMvc.perform(get("/files/download")
                        .param("filename", "missing.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Authenticated user can view PDF inline")
    void view_withAuth_ok() throws Exception {
        String token = loginAccessToken();
        mockMvc.perform(get("/files/view")
                        .param("filename", "dna-result.pdf")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("inline")));
    }
}
