package com.dna_testing_system.dev.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;

@UtilityClass
public class TimeUtils {

    public static LocalDateTime getExpiredTime(int seconds) {
        return LocalDateTime.now().plusSeconds(seconds);
    }
}
