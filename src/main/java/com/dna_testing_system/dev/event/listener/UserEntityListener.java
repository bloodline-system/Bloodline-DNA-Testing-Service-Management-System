package com.dna_testing_system.dev.event.listener;

import com.dna_testing_system.dev.constant.AttributeConstant;
import com.dna_testing_system.dev.constant.StreamConstants;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.event.type.NotificationEvent;
import com.dna_testing_system.dev.utils.ApplicationContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.PostPersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA entity listener that publishes COMPLETE_USER notification events to Redis Stream
 * when a new {@link User} is created after successful signup verification.
 */
@Slf4j
public class UserEntityListener {

    @PostPersist
    public void onUserCreated(User user) {
        // Skip Redis publishing if disabled (non-integration tests)
        if (!isRedisEnabled()) {
            return;
        }
        
        try {
            String name = user.getProfile() != null
                    ? user.getProfile().getFirstName() + " " + user.getProfile().getLastName()
                    : user.getUsername();
            String email = user.getProfile() != null ? user.getProfile().getEmail() : "";

            Map<String, Object> data = new HashMap<>();
            data.put(AttributeConstant.NAME_ATTRIBUTE, name);
            data.put(AttributeConstant.EMAIL_ATTRIBUTE, email);
            data.put(AttributeConstant.USERNAME_ATTRIBUTE, user.getUsername());
            data.put(AttributeConstant.CREATED_AT_ATTRIBUTE,
                    user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");

            NotificationEvent event = NotificationEvent.builder()
                    .eventType("COMPLETE_USER")
                    .data(data)
                    .build();

            try {
                publishToStream(event);
                log.info("Published COMPLETE_USER event for user: {}", user.getId());
            } catch (Exception redisError) {
                // Redis connection failed - acceptable in test environments without Docker
                log.warn("Could not publish COMPLETE_USER event to Redis for user: {} (Redis may be unavailable)", 
                        user.getId(), redisError);
            }
        } catch (Exception e) {
            log.error("Failed to build COMPLETE_USER event for user: {}", user.getId(), e);
        }
    }

    private void publishToStream(NotificationEvent event) throws JsonProcessingException {
        ObjectMapper objectMapper = ApplicationContextHolder.getBean(ObjectMapper.class);
        StringRedisTemplate redisTemplate = ApplicationContextHolder.getBean(StringRedisTemplate.class);
        String payload = objectMapper.writeValueAsString(event);
        redisTemplate.opsForStream().add(StreamConstants.NOTIFICATION_STREAM, Map.of("payload", payload));
    }

    /**
     * Check if Redis publishing is enabled via the app.redis.enabled property.
     * This allows non-integration tests to run without Redis being available.
     * Defaults to false for safety - only enable if explicitly configured.
     */
    private boolean isRedisEnabled() {
        try {
            Environment environment = ApplicationContextHolder.getBean(Environment.class);
            Boolean enabled = environment.getProperty("app.redis.enabled", Boolean.class);
            return enabled != null ? enabled : false; // Default to false for safety
        } catch (Exception e) {
            log.warn("Unable to determine Redis enabled status, defaulting to false (safe fallback)", e);
            return false; // Fail-safe: disable Redis if we can't read the property
        }
    }
}
