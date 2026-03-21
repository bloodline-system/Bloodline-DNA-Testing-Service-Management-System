package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AccessDeniedController {

    @GetMapping("/access-denied")
    public String getAccessDenied() {
        return "access-denied";
    }

    @GetMapping("/api/v1/errors/access-denied")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> accessDeniedApi(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Access denied", request.getRequestURI()));
    }
}
