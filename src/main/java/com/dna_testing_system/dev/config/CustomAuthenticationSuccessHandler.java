package com.dna_testing_system.dev.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Lấy role của user
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        // Redirect theo role
        if (role.equals("ROLE_MANAGER")) {
            response.sendRedirect("/manager/dashboard");
        } else if (role.equals("ROLE_ADMIN")) {
            response.sendRedirect("/admin/dashboard");
        } else if (role.equals("ROLE_STAFF")) {
            response.sendRedirect("/staff/dashboard");
        } else {
            response.sendRedirect("/user/home");
        }
    }
}
