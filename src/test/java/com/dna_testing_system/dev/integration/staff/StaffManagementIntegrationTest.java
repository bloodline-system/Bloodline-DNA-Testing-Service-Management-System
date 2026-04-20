package com.dna_testing_system.dev.integration.staff;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for staff management (admin dashboard users).
 */
@DisplayName("Staff management integration (3.1.6)")
class StaffManagementIntegrationTest extends AbstractIntegrationTest {

    private static final String MANAGER_USER = "manager2";
    private static final String MANAGER_PASS = "manager2";
    private static final String STAFF_USER = "it316staff";
    private static final String STAFF_PASS = "ItStaff123!";
    private static final String TARGET_USER = "it316target";
    private static final String TARGET_PASS = "ItTarget123!";

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
    @Autowired
    private TransactionTemplate transactionTemplate;

    private MockMvc mockMvc;
    private String targetUserId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();
        signUpRepository.deleteAll();
        try {
            flushRedis();
        } catch (Exception ignored) {
            // same as AuthIntegrationTest
        }
        seedUsers();
    }

    private void seedUsers() {
        Role roleManager = roleRepository.findByRoleName("MANAGER").orElseThrow();
        Role roleStaff = roleRepository.findByRoleName("STAFF").orElseThrow();

        persistUser(MANAGER_USER, MANAGER_PASS, roleManager);
        persistUser(STAFF_USER, STAFF_PASS, roleStaff);
        User target = persistUser(TARGET_USER, TARGET_PASS, roleStaff);
        targetUserId = target.getId();
    }

    private User persistUser(String username, String rawPassword, Role role) {
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .isActive(true)
                .userRoles(new HashSet<>())
                .build();
        user = userRepository.save(user);

        UserRole ur = UserRole.builder()
                .user(user)
                .role(role)
                .isActive(true)
                .build();
        userRoleRepository.save(ur);
        user.getUserRoles().add(ur);
        userRepository.save(user);

        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName("IT")
                .lastName(username)
                .email(username + "@example.com")
                .phoneNumber("0900000001")
                .build();
        userProfileRepository.save(profile);
        user.setProfile(profile);
        return userRepository.save(user);
    }

    private String loginAccessToken(String username, String password) throws Exception {
        AuthenticationRequestDTO login = AuthenticationRequestDTO.builder()
                .username(username)
                .password(password)
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
        assertThat(body.getData().getAccessToken()).isNotBlank();
        return body.getData().getAccessToken();
    }

    @Test
    @DisplayName("MANAGER can list dashboard users")
    void manager_listsUsers_ok() throws Exception {
        String token = loginAccessToken(MANAGER_USER, MANAGER_PASS);
        mockMvc.perform(get("/api/v1/admin/dashboard/users")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.users").isArray());
    }

    @Test
    @DisplayName("STAFF cannot access dashboard users (403)")
    void staff_listsUsers_forbidden() throws Exception {
        String token = loginAccessToken(STAFF_USER, STAFF_PASS);
        mockMvc.perform(get("/api/v1/admin/dashboard/users")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER can update target user role to MANAGER")
    void manager_updatesUserRole_ok() throws Exception {
        String token = loginAccessToken(MANAGER_USER, MANAGER_PASS);
        mockMvc.perform(put("/api/v1/admin/dashboard/users/{userId}", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MANAGER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User updated successfully"));

        transactionTemplate.executeWithoutResult(status -> {
            User u = userRepository.findById(targetUserId).orElseThrow();
            assertThat(u.getUserRoles()).isNotEmpty();
            assertThat(u.getUserRoles().iterator().next().getRole().getRoleName()).isEqualTo("MANAGER");
        });
    }

    @Test
    @DisplayName("MANAGER can get user by id")
    void manager_getUserById_ok() throws Exception {
        String token = loginAccessToken(MANAGER_USER, MANAGER_PASS);
        mockMvc.perform(get("/api/v1/admin/dashboard/users/{userId}", targetUserId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(TARGET_USER));
    }

    @Test
    @DisplayName("MANAGER can update target user contact info")
    void manager_updatesUserContact_ok() throws Exception {
        String token = loginAccessToken(MANAGER_USER, MANAGER_PASS);
        mockMvc.perform(put("/api/v1/admin/dashboard/users/{userId}", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"updated.staff@example.com\",\"phone\":\"0912000001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("User updated successfully"));

        transactionTemplate.executeWithoutResult(status -> {
            User u = userRepository.findById(targetUserId).orElseThrow();
            assertThat(u.getProfile()).isNotNull();
            assertThat(u.getProfile().getEmail()).isEqualTo("updated.staff@example.com");
            assertThat(u.getProfile().getPhoneNumber()).isEqualTo("0912000001");
        });
    }

    @Test
    @DisplayName("MANAGER can deactivate then reactivate staff account")
    void manager_deactivateReactivateUser_ok() throws Exception {
        String token = loginAccessToken(MANAGER_USER, MANAGER_PASS);

        mockMvc.perform(put("/api/v1/admin/dashboard/users/{userId}", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        transactionTemplate.executeWithoutResult(status -> {
            User u = userRepository.findById(targetUserId).orElseThrow();
            assertThat(u.getIsActive()).isFalse();
        });

        mockMvc.perform(put("/api/v1/admin/dashboard/users/{userId}", targetUserId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        transactionTemplate.executeWithoutResult(status -> {
            User u = userRepository.findById(targetUserId).orElseThrow();
            assertThat(u.getIsActive()).isTrue();
        });
    }

    @Test
    @DisplayName("MANAGER can list users with search")
    void manager_listsUsers_withSearch_ok() throws Exception {
        String token = loginAccessToken(MANAGER_USER, MANAGER_PASS);
        mockMvc.perform(get("/api/v1/admin/dashboard/users")
                        .param("search", "target")
                        .param("page", "0")
                        .param("size", "20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.users").isArray());
    }

    @Test
    @DisplayName("STAFF cannot update dashboard user (403)")
    void staff_updatesUser_forbidden() throws Exception {
        String token = loginAccessToken(STAFF_USER, STAFF_PASS);
        mockMvc.perform(put("/api/v1/admin/dashboard/users/{userId}", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MANAGER\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
