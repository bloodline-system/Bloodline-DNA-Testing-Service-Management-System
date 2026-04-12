package com.dna_testing_system.dev.redis;

import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Test that verifies non-integration tests (unit tests, service tests, etc.)
 * can run without Redis being available.
 * 
 * This test uses a non-integration test configuration where:
 * - app.redis.enabled = false
 * - Redis connection details are intentionally invalid/unreachable
 * 
 * This ensures that regular tests don't require Docker and can run in CI
 * without a Redis container.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.redis.enabled=false",
        "spring.redis.host=invalid",
        "spring.redis.port=6379"
})
class RedisPublishingDisabledTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesUserWithoutPublishingToRedis() {
        User user = User.builder()
                .username("redis-disabled-user")
                .passwordHash("hash")
                .isActive(true)
                .build();

        userRepository.save(user);
    }
}
