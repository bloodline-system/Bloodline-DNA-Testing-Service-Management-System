package com.dna_testing_system.dev.controller.api;

import com.dna_testing_system.dev.config.ApplicationInitConfig;
import com.dna_testing_system.dev.config.RedisStreamConfig;
import com.dna_testing_system.dev.entity.Role;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.entity.UserRole;
import com.dna_testing_system.dev.security.JwtAuthenticationFilter;
import com.dna_testing_system.dev.service.OrderTaskManagementService;
import com.dna_testing_system.dev.service.SystemReportService;
import com.dna_testing_system.dev.service.UserProfileService;
import com.dna_testing_system.dev.service.service.MedicalServiceManageService;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Bộ test cho các API quản lý nhân viên trong Admin Dashboard
// Lưu ý:
// - Chỉ test layer controller (sử dụng @WebMvcTest + MockMvc)
// - Không đụng tới DB thật, mọi dependency đều được mock bằng @MockitoBean
// - Không sửa code controller, chỉ kiểm tra hành vi hiện tại (response wrapper, status, message, data)
@WebMvcTest(controllers = ApiAdminDashboardRestController.class, excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = ApplicationInitConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RedisStreamConfig.class)
})
@AutoConfigureMockMvc(addFilters = false)
class ApiAdminDashboardRestControllerTest {

        @Autowired
        MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        MedicalServiceManageService medicalServiceManageService;
        @MockitoBean
        OrderTaskManagementService orderTaskManagementService;
        @MockitoBean
        SystemReportService systemReportService;
        @MockitoBean
        UserProfileService userProfileService;
        @MockitoBean
        UserRepository userRepository;
        @MockitoBean
        RoleRepository roleRepository;
        @MockitoBean
        UserRoleRepository userRoleRepository;

        // Helper tạo nhanh User + UserProfile để dùng trong các test
        private static User user(String id, String username, String email) {
                User u = User.builder()
                                .id(id)
                                .username(username)
                                .isActive(true)
                                .build();
                if (email != null) {
                        UserProfile p = UserProfile.builder()
                                        .email(email)
                                        .phoneNumber("0900000000")
                                        .build();
                        p.setUser(u);
                        u.setProfile(p);
                }
                return u;
        }

        // ======================= GET /users =======================

        // Trường hợp mặc định: không truyền search, trả về đầy đủ danh sách + thông tin
        // phân trang
        @Test
        @WithMockUser(roles = "ADMIN")
        void getUsersList_default_returnsWrapperAndPagination() throws Exception {
                when(userRepository.findAll()).thenReturn(List.of(
                                user("u1", "staff1", "staff1@ex.com"),
                                user("u2", "manager1", "manager1@ex.com")));
                when(userRepository.findUsersByRoleName("ADMIN")).thenReturn(List.of());
                when(userRepository.findUsersByRoleName("MANAGER"))
                                .thenReturn(List.of(user("u2", "manager1", "manager1@ex.com")));
                when(userRepository.findUsersByRoleName("STAFF"))
                                .thenReturn(List.of(user("u1", "staff1", "staff1@ex.com")));
                when(userRepository.findUsersByRoleName("CUSTOMER")).thenReturn(List.of());

                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users")
                                .param("page", "0")
                                .param("size", "20")
                                .param("sortBy", "createdAt")
                                .param("sortDir", "desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("Users list loaded"))
                                .andExpect(jsonPath("$.data.users", hasSize(2)))
                                .andExpect(jsonPath("$.data.totalElements", is(2)))
                                .andExpect(jsonPath("$.data.currentPage", is(0)))
                                .andExpect(jsonPath("$.data.pageSize", is(20)))
                                .andExpect(jsonPath("$.data.totalPages", is(1)));
        }

        // Khi truyền search -> danh sách được filter theo username hoặc email
        // (contains, không phân biệt hoa thường)
        @Test
        @WithMockUser(roles = "ADMIN")
        void getUsersList_withSearch_filtersByUsernameOrEmail() throws Exception {
                when(userRepository.findAll()).thenReturn(List.of(
                                user("u1", "staff1", "staff1@ex.com"),
                                user("u2", "manager1", "manager1@ex.com")));
                when(userRepository.findUsersByRoleName(any())).thenReturn(List.of());

                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users")
                                .param("search", "staff"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.users", hasSize(1)))
                                .andExpect(jsonPath("$.data.totalElements", is(1)))
                                .andExpect(jsonPath("$.data.users[0].username").value("staff1"));
        }

        // ======================= GET /users/{id} =======================

        // User không tồn tại -> trả về 404 với wrapper ApiResponse.error
        @Test
        @WithMockUser(roles = "ADMIN")
        void getUserById_returns404WhenNotFound() throws Exception {
                when(userRepository.findById("ghost")).thenReturn(Optional.empty());

                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users/{id}", "ghost"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404))
                                .andExpect(jsonPath("$.message").value("User not found"));
        }

        // User tồn tại -> trả về 200 với thông tin user trong data
        @Test
        @WithMockUser(roles = "ADMIN")
        void getUserById_returnsOkWhenFound() throws Exception {
                when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "staff1", "staff1@ex.com")));

                mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/admin/dashboard/users/{id}", "u1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("User loaded"))
                                .andExpect(jsonPath("$.data.id").value("u1"))
                                .andExpect(jsonPath("$.data.username").value("staff1"));
        }

        // ======================= PUT /users/{id} =======================

        // Cập nhật đủ các trường: email, phone, active, role
        // - Kiểm tra response 200 + message
        // - Dùng ArgumentCaptor để verify User sau khi save đã được cập nhật đúng
        @Test
        @WithMockUser(roles = "ADMIN")
        void updateUser_updatesEmailPhoneActiveAndRole() throws Exception {
                User existing = user("u1", "staff1", "old@ex.com");
                Role staffRole = Role.builder().id(1L).roleName("STAFF").build();
                UserRole userRole = UserRole.builder().id(1L).user(existing)
                                .role(Role.builder().id(2L).roleName("CUSTOMER").build()).build();
                existing.setUserRoles(Set.of(userRole));

                when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
                when(roleRepository.findByRoleName("STAFF")).thenReturn(Optional.of(staffRole));
                when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

                String json = """
                                {
                                  "email": "new@ex.com",
                                  "phone": "0909123456",
                                  "active": false,
                                  "role": "STAFF"
                                }
                                """;

                mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admin/dashboard/users/{id}", "u1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("User updated successfully"))
                                .andExpect(jsonPath("$.data.id").value("u1"))
                                .andExpect(jsonPath("$.data.isActive").value(false));

                ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
                verify(userRepository).save(captor.capture());
                User saved = captor.getValue();
                assertEquals("new@ex.com", saved.getProfile().getEmail());
                assertEquals("0909123456", saved.getProfile().getPhoneNumber());
                assertEquals(false, saved.getIsActive());
                assertEquals("STAFF", saved.getUserRoles().stream().findFirst().orElseThrow().getRole().getRoleName());
        }

        // Khi userId không tồn tại -> controller trả về 404 + wrapper error
        @Test
        @WithMockUser(roles = "ADMIN")
        void updateUser_returns404WhenUserNotFound() throws Exception {
                when(userRepository.findById("ghost")).thenReturn(Optional.empty());

                mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admin/dashboard/users/{id}", "ghost")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(java.util.Map.of("active", true))))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.code").value(404))
                                .andExpect(jsonPath("$.message").value("User not found"));
        }

        // Khi role trong request không tìm thấy trong DB -> RoleRepository ném
        // RuntimeException
        // Controller bắt Exception chung và trả về 500 + message "Unable to update
        // user"
        @Test
        @WithMockUser(roles = "ADMIN")
        void updateUser_returns500WhenRoleNotFound() throws Exception {
                User existing = user("u1", "staff1", "old@ex.com");
                UserRole userRole = UserRole.builder().id(1L).user(existing)
                                .role(Role.builder().id(2L).roleName("CUSTOMER").build()).build();
                existing.setUserRoles(Set.of(userRole));

                when(userRepository.findById("u1")).thenReturn(Optional.of(existing));
                when(roleRepository.findByRoleName("STAFF")).thenReturn(Optional.empty());

                mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/admin/dashboard/users/{id}", "u1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(java.util.Map.of("role", "STAFF"))))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.code").value(500))
                                .andExpect(jsonPath("$.message").value("Unable to update user"))
                                .andExpect(jsonPath("$.path").value("/api/v1/admin/dashboard/users/u1"));

                verify(userRepository, never()).save(any());
        }
}
