package com.dna_testing_system.dev.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordUtil {
    private final static PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    public static boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
