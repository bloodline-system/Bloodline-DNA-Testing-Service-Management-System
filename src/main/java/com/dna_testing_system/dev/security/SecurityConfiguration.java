package com.dna_testing_system.dev.security;

import com.dna_testing_system.dev.config.CustomAuthenticationSuccessHandler;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.service.impl.UserDetailsServiceImpl;
import com.dna_testing_system.dev.utils.PasswordUtil;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfiguration {

    UserDetailsServiceImpl userDetailsService;
    CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/css/**", "/js/**", "/images/**", "/webjars/**",
                                "/", "/register", "/login", "/error", "/assets/**", "/uploads/**", "/api/**", "/ws/**", "/cancel", "/public/**",
                                "/files/**", "/uploads_information/**"  // Add permission for file directories
                        ).permitAll()
                        .requestMatchers("/manager/**", "/manager/services/**", "/manager/service-types/**").hasAnyRole(RoleType.MANAGER.name(), RoleType.ADMIN.name())
                        .requestMatchers("/admin/**").hasAnyRole(RoleType.ADMIN.name())
                        .requestMatchers("/staff/**").hasAnyRole(RoleType.STAFF.name(), RoleType.MANAGER.name(), RoleType.ADMIN.name())
                        .requestMatchers("/user/**").authenticated()
                        .anyRequest().permitAll()
                )
                .csrf(csrf -> csrf
                    .ignoringRequestMatchers("/register")
                    .ignoringRequestMatchers("/staff/**")
                        .ignoringRequestMatchers("/user/**")
                        .ignoringRequestMatchers("/admin/**")
                        .ignoringRequestMatchers("/manager/**")
                    .ignoringRequestMatchers("/files/**")              // Allow file operations
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(customAuthenticationSuccessHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("uniqueAndSecretKey")
                        .tokenValiditySeconds(86400)
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/access-denied")
                )
                // --- BẮT ĐẦU PHẦN THÊM MỚI ---
                // Thêm cấu hình headers để kiểm soát cache, giúp trình duyệt hoạt động đúng
                .headers(headers -> headers
                        .cacheControl(cache -> {}) // Kích hoạt quản lý Cache-Control mặc định
                        .frameOptions(frameOptions -> frameOptions.sameOrigin()) // Chống clickjacking
                );
        // --- KẾT THÚC PHẦN THÊM MỚI ---


        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(PasswordUtil.getPasswordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}