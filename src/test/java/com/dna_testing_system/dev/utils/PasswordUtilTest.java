package com.dna_testing_system.dev.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void encodeShouldReturnNonNullAndDifferentFromRaw() {
        String rawPassword = "mySecret123";
        String encoded = PasswordUtil.encode(rawPassword);

        assertNotNull(encoded, "Encoded password should not be null");
        assertNotEquals(rawPassword, encoded, "Encoded password should not equal raw password");
    }

    @Test
    void matchesShouldReturnTrueForCorrectPassword() {
        String rawPassword = "mySecret123";
        String encoded = PasswordUtil.encode(rawPassword);

        assertTrue(PasswordUtil.matches(rawPassword, encoded), "Password should match encoded hash");
    }

    @Test
    void matchesShouldReturnFalseForIncorrectPassword() {
        String rawPassword = "mySecret123";
        String wrongPassword = "wrongPassword";
        String encoded = PasswordUtil.encode(rawPassword);

        assertFalse(PasswordUtil.matches(wrongPassword, encoded), "Wrong password should not match encoded hash");
    }
}