package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.AuthenticationRequest;
import com.dna_testing_system.dev.dto.request.RegisterRequest;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for handling authentication operations
 */
@Controller
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    
    AuthenticationService authenticationService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        // Redirect to home if user is already authenticated
        if (isAuthenticated()) {
            return "redirect:/index";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "signup";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "signup";
        }
        User user = authenticationService.register(request);
        return "redirect:/login";

//        // Check if username is available
//        if (!authenticationService.isUsernameAvailable(request.getUsername())) {
//            model.addAttribute("errorMessage", "Username already exists");
//            return "signup";
//        }
//
//        try {
//            User user = authenticationService.register(request);
//            redirectAttributes.addFlashAttribute("successMessage",
//                    "Registration successful! Please login with your credentials.");
//            return "redirect:/login";
//        } catch (Exception e) {
//            model.addAttribute("errorMessage", e.getMessage());
//            return "signup";
//        }
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        // Redirect to home if user is already authenticated
        if (isAuthenticated()) {
            return "redirect:/user/home";
        }
        model.addAttribute("authRequest", new AuthenticationRequest());
        return "signin";
    }
    
    // No need for the POST /login handler anymore since Spring Security handles it
    
    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               !(authentication instanceof AnonymousAuthenticationToken);
    }
}
