package com.dna_testing_system.dev.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeUtilsTest {

    @Test
    void getExpiredTime() {
        int seconds = 60;
        LocalDateTime expiredTime = TimeUtils.getExpiredTime(seconds);
        LocalDateTime expectedTime = LocalDateTime.now().plusSeconds(seconds);

        // Allow a small margin of error for the time difference
        assertTrue(expiredTime.isAfter(expectedTime.minusSeconds(1)) && expiredTime.isBefore(expectedTime.plusSeconds(1)));
    }
}