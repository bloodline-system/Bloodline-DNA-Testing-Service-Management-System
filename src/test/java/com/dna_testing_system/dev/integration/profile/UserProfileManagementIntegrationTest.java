package com.dna_testing_system.dev.integration.profile;

import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.integration.common.AbstractIntegrationTest;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User Profile Management - Integration")
@Transactional
class UserProfileManagementIntegrationTest extends AbstractIntegrationTest {

    private static final String CUSTOMER_USERNAME = "it_profile_user";
    private static final String CUSTOMER_EMAIL = "it_profile_user@example.com";

    private static final String ADMIN_USERNAME = "it_admin_user";
    private static final String ADMIN_EMAIL = "it_admin_user@example.com";

    @DynamicPropertySource
    static void disableRedisPublishing(DynamicPropertyRegistry registry) {
        // Keep integration tests focused on MySQL; avoid Redis stream/listener dependencies.
        registry.add("app.redis.enabled", () -> false);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    /**
     * Prevent Redis stream listener from starting during @SpringBootTest.
     * The real bean comes from RedisStreamConfig.mailStreamListenerContainer(...).
     */
    @MockitoBean
    StreamMessageListenerContainer<String, MapRecord<String, String, String>> mailStreamListenerContainer;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        ensureUserWithProfile(CUSTOMER_USERNAME, "Integration", "Customer", CUSTOMER_EMAIL);
        ensureUserWithProfile(ADMIN_USERNAME, "Integration", "Admin", ADMIN_EMAIL);
    }

    private User ensureUserWithProfile(String username, String firstName, String lastName, String email) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = userRepository.save(User.builder()
                    .username(username)
                    .passwordHash("test-hash")
                    .isActive(true)
                    .build());
        }

        if (user.getProfile() == null) {
            UserProfile profile = UserProfile.builder()
                    .user(user)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .phoneNumber("0123456789")
                    .profileImageUrl("/uploads/test.png")
                    .build();
            userProfileRepository.save(profile);
            user.setProfile(profile);
            userRepository.save(user);
        }

        return userRepository.findByUsername(username).orElseThrow();
    }

    @Test
    @WithMockUser(username = CUSTOMER_USERNAME, roles = "CUSTOMER")
    void getMyProfile_returnsProfile() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Get my profile successfully"))
                .andExpect(jsonPath("$.data.username").value(CUSTOMER_USERNAME))
                .andExpect(jsonPath("$.data.email").value(CUSTOMER_EMAIL));
    }

    @Test
    @WithMockUser(username = CUSTOMER_USERNAME, roles = "CUSTOMER")
    void updateProfileJson_persistsChanges() throws Exception {
        UserProfileRequest update = UserProfileRequest.builder()
                .firstName("Updated")
                .lastName("User")
                .email(CUSTOMER_EMAIL)
                .phoneNumber("+84 123 456")
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .build();

        mockMvc.perform(put("/api/v1/profiles/{username}", CUSTOMER_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Update profile successfully"))
                .andExpect(jsonPath("$.data.username").value(CUSTOMER_USERNAME))
                .andExpect(jsonPath("$.data.firstName").value("Updated"))
                .andExpect(jsonPath("$.data.lastName").value("User"))
                .andExpect(jsonPath("$.data.email").value(CUSTOMER_EMAIL))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1995-05-20"));

        User reloaded = userRepository.findByUsername(CUSTOMER_USERNAME).orElseThrow();
        assertThat(reloaded.getProfile()).isNotNull();
        assertThat(reloaded.getProfile().getFirstName()).isEqualTo("Updated");
        assertThat(reloaded.getProfile().getLastName()).isEqualTo("User");
        assertThat(reloaded.getProfile().getPhoneNumber()).isEqualTo("+84 123 456");
        assertThat(reloaded.getProfile().getDateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 20));
    }

    @Test
    @WithMockUser(username = CUSTOMER_USERNAME, roles = "CUSTOMER")
    void searchProfiles_findsByName() throws Exception {
        UserProfileRequest update = UserProfileRequest.builder()
                .firstName("Searchable")
                .lastName("Person")
                .email(CUSTOMER_EMAIL)
                .build();

        mockMvc.perform(put("/api/v1/profiles/{username}", CUSTOMER_USERNAME)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/profiles/search")
                        .param("name", "Searchable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Search profiles successfully"))
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.data[*].username", hasItem(CUSTOMER_USERNAME)));
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = "ADMIN")
    void adminGetAllProfiles_returnsList() throws Exception {
        mockMvc.perform(get("/api/v1/admin/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Profiles loaded successfully"))
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0)));
    }

    @Test
    @WithMockUser(username = ADMIN_USERNAME, roles = "ADMIN")
    void adminUpdateProfile_multipartWithoutEmail_preservesEmailAndUpdatesNames() throws Exception {
        UserProfileRequest update = UserProfileRequest.builder()
                .firstName("AdminEdited")
                .lastName("Customer")
                .phoneNumber("0999888777")
                .build();

        MockMultipartFile profilePart = new MockMultipartFile(
                "profile",
                "profile.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(update)
        );

        mockMvc.perform(multipart("/api/v1/admin/profiles/{username}", CUSTOMER_USERNAME)
                        .file(profilePart)
                        .with(req -> {
                            req.setMethod("PUT");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.username").value(CUSTOMER_USERNAME))
                .andExpect(jsonPath("$.data.firstName").value("AdminEdited"))
                .andExpect(jsonPath("$.data.lastName").value("Customer"))
                .andExpect(jsonPath("$.data.email").value(CUSTOMER_EMAIL));

        User reloaded = userRepository.findByUsername(CUSTOMER_USERNAME).orElseThrow();
        assertThat(reloaded.getProfile()).isNotNull();
        assertThat(reloaded.getProfile().getEmail()).isEqualTo(CUSTOMER_EMAIL);
        assertThat(reloaded.getProfile().getFirstName()).isEqualTo("AdminEdited");
        assertThat(reloaded.getProfile().getLastName()).isEqualTo("Customer");
    }
}
