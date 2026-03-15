package com.dna_testing_system.dev.enums;

public enum SignUpStatus {
    PENDING,      // Waiting for email verification
    VERIFIED,     // Email verified, account active
    EXPIRED,      // Verification token expired
    CANCELLED     // Signup process cancelled
}
