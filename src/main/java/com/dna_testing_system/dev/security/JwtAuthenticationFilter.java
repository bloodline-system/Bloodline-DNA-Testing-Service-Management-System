package com.dna_testing_system.dev.security;

import com.dna_testing_system.dev.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dna_testing_system.dev.service.auth.JwtKeyService;
import com.dna_testing_system.dev.service.auth.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtKeyService jwtKeyService;
    private final TokenBlacklistService tokenBlacklistService;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getServletPath();

        if (path.startsWith("/api/v1/auth/") && !path.equals("/api/v1/auth/logout")) {
            return true;
        }

        return "/".equals(path)
            || path.startsWith("/actuator/")
            || path.startsWith("/oauth2/")
            || "/login/oauth2/code/google".equals(path)
            || path.startsWith("/api/debug/")
            || path.startsWith("/swagger-ui/")
            || path.startsWith("/v3/api-docs/")
            || "/swagger-ui.html".equals(path)
            || "/docs".equals(path);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtKeyService.validateToken(token) || tokenBlacklistService.isAccessTokenBlacklisted(token)) {
            ApiResponse<Void> apiResponse = ApiResponse.error(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired token",
                    request.getRequestURI()
            );

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), apiResponse);
            return;
        }

        // Build authorities từ roles + permissions
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        List<String> roles = jwtKeyService.extractRoles(token);
        if (roles != null) {
            roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .forEach(authorities::add);
        }

        List<String> permissions = jwtKeyService.extractPermissions(token);
        if (permissions != null) {
            permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add);
        }

        String username = jwtKeyService.extractUsername(token);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}