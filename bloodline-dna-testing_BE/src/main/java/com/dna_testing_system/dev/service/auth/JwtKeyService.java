package com.dna_testing_system.dev.service.auth;



import com.dna_testing_system.dev.entity.User;
import java.time.Duration;
import java.util.List;

public interface JwtKeyService {

    String generateToken(User user, Duration expiry);

    String generateRefreshToken(User user, Duration expiry);

    boolean validateToken(String token);
    List<String> extractRoles(String token);

    List<String> extractPermissions(String token);

    String extractUsername(String token);
}