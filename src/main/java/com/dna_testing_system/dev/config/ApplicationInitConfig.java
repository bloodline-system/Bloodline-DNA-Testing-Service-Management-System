package com.dna_testing_system.dev.config;

import com.dna_testing_system.dev.entity.Role;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import com.dna_testing_system.dev.entity.UserRole;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.UserProfileRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.repository.UserRoleRepository;
import com.dna_testing_system.dev.utils.PasswordUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.awt.*;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Configuration
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationInitConfig implements ApplicationRunner, WebMvcConfigurer {
    final RoleRepository roleRepository;
    final UserRepository userRepository;
    final UserRoleRepository userRoleRepository;
    final UserProfileRepository userProfileRepository;
    @Value("${application.base-url}")
    String baseURL;

    public void run(ApplicationArguments args) throws Exception {
        initRoleDatabase();
        createAdminDefault();
        createManagerDefault();
        createStaffDefault();
        launchBrowser();
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Cấu hình chi tiết hơn cho static resources
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCachePeriod(0);

        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(0);

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(0);

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(0);

        // Cấu hình cho tất cả static resources
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root URL to /index

        registry.addViewController("/index").setViewName("index");
    }


    private void initRoleDatabase() {
        for (RoleType roleType : RoleType.values()) {
            addRoleIfNotExists(roleType);
        }
    }

    private void addRoleIfNotExists(RoleType roleType) {
        if (!roleRepository.existsByRoleName(roleType.name())) {
            Role role = Role.builder()
                    .roleName(roleType.name())
                    .roleDescription(roleType.getDescription())
                    .isActive(true)
                    .build();
            roleRepository.save(role);
        }
    }
    private void createStaffDefault() {
        if (!userRepository.existsByUsername("staff1")) {
            Role role = roleRepository.findByRoleName(RoleType.STAFF.name());

            // Create and save the User entity with minimal information
            User user = User.builder()
                    .username("staff1")
                    .passwordHash(PasswordUtil.encode("staff"))
                    .isActive(true)
                    .userRoles(new HashSet<>())
                    .build();

            user = userRepository.save(user);

            UserRole userRoleForCreate = UserRole.builder()
                    .user(user)
                    .role(role)
                    .isActive(true)
                    .build();
            UserRole userRole = userRoleRepository.save(userRoleForCreate);
            user.getUserRoles().add(userRole);
            userRepository.save(user);

            // Create and save minimal UserProfile
            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .firstName("New")
                    .lastName("STAFF")
                    .email("staff@email.com")
                    .phoneNumber("1309103213")
                    .profileImageUrl("ajsdhfjahsasdf")
                    .build();

            userProfileRepository.save(userProfile);

            user.setUserProfile(userProfile);
            userRepository.save(user);
        }
        if (!userRepository.existsByUsername("staff2")) {
            Role role = roleRepository.findByRoleName(RoleType.STAFF.name());

            // Create and save the User entity with minimal information
            User user = User.builder()
                    .username("staff2")
                    .passwordHash(PasswordUtil.encode("staff2"))
                    .isActive(true)
                    .userRoles(new HashSet<>())
                    .build();

            user = userRepository.save(user);

            UserRole userRoleForCreate = UserRole.builder()
                    .user(user)
                    .role(role)
                    .isActive(true)
                    .build();
            UserRole userRole = userRoleRepository.save(userRoleForCreate);
            user.getUserRoles().add(userRole);
            userRepository.save(user);

            // Create and save minimal UserProfile
            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .firstName("New")
                    .lastName("STAFF")
                    .email("staff2@email.com")
                    .phoneNumber("1309103213")
                    .profileImageUrl("ajsdhfjahsasdf")
                    .build();

            userProfileRepository.save(userProfile);

            user.setUserProfile(userProfile);
            userRepository.save(user);
        }
    }

    private void createManagerDefault() {
        if (!userRepository.existsByUsername("manager")) {
            Role role = roleRepository.findByRoleName(RoleType.MANAGER.name());

            // Create and save the User entity with minimal information
            User user = User.builder()
                    .username("manager")
                    .passwordHash(PasswordUtil.encode("manager"))
                    .isActive(true)
                    .userRoles(new HashSet<>())
                    .build();

            user = userRepository.save(user);

            UserRole userRoleForCreate = UserRole.builder()
                    .user(user)
                    .role(role)
                    .isActive(true)
                    .build();
            UserRole userRole = userRoleRepository.save(userRoleForCreate);
            user.getUserRoles().add(userRole);
            userRepository.save(user);

            // Create and save minimal UserProfile
            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .firstName("MANAGER")
                    .email("manager@email.com")
                    .build();

            userProfileRepository.save(userProfile);

            user.setUserProfile(userProfile);
            userRepository.save(user);
        }
        if (!userRepository.existsByUsername("manager2")) {
            Role role = roleRepository.findByRoleName(RoleType.MANAGER.name());

            // Create and save the User entity with minimal information
            User user = User.builder()
                    .username("manager2")
                    .passwordHash(PasswordUtil.encode("manager2"))
                    .isActive(true)
                    .userRoles(new HashSet<>())
                    .build();

            user = userRepository.save(user);

            UserRole userRoleForCreate = UserRole.builder()
                    .user(user)
                    .role(role)
                    .isActive(true)
                    .build();
            UserRole userRole = userRoleRepository.save(userRoleForCreate);
            user.getUserRoles().add(userRole);
            userRepository.save(user);

            // Create and save minimal UserProfile
            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .firstName("MANAGER")
                    .email("manager2@email.com")
                    .build();

            userProfileRepository.save(userProfile);

            user.setUserProfile(userProfile);
            userRepository.save(user);
        }
    }

    private void createAdminDefault() {
        if (!userRepository.existsByUsername("admin")) {
            Role role = roleRepository.findByRoleName(RoleType.ADMIN.name());

            // Create and save the User entity with minimal information
            User user = User.builder()
                    .username("admin")
                    .passwordHash(PasswordUtil.encode("admin"))
                    .isActive(true)
                    .userRoles(new HashSet<>())
                    .build();

            user = userRepository.save(user);

            UserRole userRoleForCreate = UserRole.builder()
                    .user(user)
                    .role(role)
                    .isActive(true)
                    .build();
            UserRole userRole = userRoleRepository.save(userRoleForCreate);
            user.getUserRoles().add(userRole);
            userRepository.save(user);

            // Create and save minimal UserProfile
            UserProfile userProfile = UserProfile.builder()
                    .user(user)
                    .firstName("ADMIN")
                    .email("admin@gmail.com")
                    .build();

            userProfileRepository.save(userProfile);

            user.setUserProfile(userProfile);
            userRepository.save(user);
        }
    }

    private void launchBrowser() throws Exception{
        if (Desktop.isDesktopSupported())
            Desktop.getDesktop().browse(new URI(baseURL));
        else {
            log.warn("Desktop is not supported, cannot launch Browser");
            log.info("Starting to launch Browser with OS Call System...");
            String os = System.getProperty("os.name").toLowerCase();
            Runtime runtime = Runtime.getRuntime();
            if (os.contains("win")) {
                // Windows
                runtime.exec("rundll32 url.dll,FileProtocolHandler " + baseURL);
            } else {
                log.warn("Unsupported OS. Cannot open browser automatically.");
            }
        }
    }
}