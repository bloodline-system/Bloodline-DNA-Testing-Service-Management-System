package com.dna_testing_system.dev.utils;

import com.dna_testing_system.dev.constant.OptConstants;
import com.dna_testing_system.dev.exception.OptFailException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptGenerateUtilsTest {

    @Test
    void generateOtpIsValid() {
        // Given
        int otpLength = 6;
        // When
        String otp = OptGenerateUtils.generateOtp();
        // Then
        assertNotNull(otp);
        assertEquals(otpLength, otp.length());
        assertTrue(otp.matches("\\d{" + otpLength + "}"), "OTP should be numeric and of correct length");

    }

    @Test
    void shouldThrowExceptionIfResendTooSoon() {
        LocalDateTime lastSentTime = LocalDateTime.now().minusSeconds(30); // 30s trước

        OptFailException exception = assertThrows(
                OptFailException.class,
                () -> OptGenerateUtils.validateResendOtp(lastSentTime)
        );

        assertEquals("Please wait 60 seconds before requesting another OTP", exception.getMessage());
    }

    @Test
    void shouldPassIfResendAfterInterval() {
        LocalDateTime lastSentTime = LocalDateTime.now().minusSeconds(OptConstants.RESEND_INTERVAL_SECONDS + 1);

        assertDoesNotThrow(() -> OptGenerateUtils.validateResendOtp(lastSentTime));
    }

    @Test
    void shouldThrowExceptionIfValidateTimeIsNull() {
        // Given
        LocalDateTime lastSentTime = null;
        // When & Then
        assertThrows(OptFailException.class, () -> OptGenerateUtils.validateResendOtp(lastSentTime));
    }

    @Test
    void shouldThrowExceptionIfValueAttemptIsNegative() {
        int attempts = -1;

        OptFailException exception = assertThrows(
                OptFailException.class,
                () -> OptGenerateUtils.validateOtpAttempts(attempts)
        );

        assertEquals("Attempts cannot be negative", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionIfValidateAttemptsExceedsMax() {
        int attempts = OptConstants.MAX_ATTEMPTS;

        OptFailException exception = assertThrows(
                OptFailException.class,
                () -> OptGenerateUtils.validateOtpAttempts(attempts)
        );
        assertEquals("Too many incorrect OTP attempts", exception.getMessage());
    }
}