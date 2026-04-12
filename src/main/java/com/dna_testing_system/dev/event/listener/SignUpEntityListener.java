package com.dna_testing_system.dev.event.listener;

import com.dna_testing_system.dev.constant.AttributeConstant;
import com.dna_testing_system.dev.constant.StreamConstants;
import com.dna_testing_system.dev.entity.SignUp;
import com.dna_testing_system.dev.enums.SignUpStatus;
import com.dna_testing_system.dev.event.type.NotificationEvent;
import com.dna_testing_system.dev.utils.ApplicationContextHolder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * JPA entity listener that publishes VERIFY_USER notification events to Redis Stream.
 * <p>
 * Triggered on insert (new registration) and on update when status is still PENDING (resend OTP).
 */
@Slf4j
public class SignUpEntityListener {

    @PostPersist
    public void onSignUpCreated(SignUp signUp) {
        // Skip Redis publishing if disabled (non-integration tests)
        if (!isRedisEnabled()) {
            return;
        }
        publishVerifyUserEvent(signUp);
    }

    @PostUpdate
    public void onSignUpUpdated(SignUp signUp) {
        // Skip Redis publishing if disabled (non-integration tests)
        if (!isRedisEnabled()) {
            return;
        }
        if (SignUpStatus.PENDING.equals(signUp.getStatus())) {
            publishVerifyUserEvent(signUp);
        }
    }

    private void publishVerifyUserEvent(SignUp signUp) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put(AttributeConstant.NAME_ATTRIBUTE, signUp.getFirstName() + " " + signUp.getLastName());
            data.put(AttributeConstant.EMAIL_ATTRIBUTE, signUp.getEmail());
            data.put(AttributeConstant.VERIFY_TOKEN_ATTRIBUTE, signUp.getCurrentVerificationToken());
            data.put(AttributeConstant.EXPIRED_DATE_ATTRIBUTE, signUp.getExpiredVerificationTokenDate().toString());

            NotificationEvent event = NotificationEvent.builder()
                    .eventType("VERIFY_USER")
                    .data(data)
                    .build();

            publishToStream(event);
            log.debug("Published VERIFY_USER event for signup: {}", signUp.getId());
        } catch (Exception e) {
            log.debug("Failed to publish VERIFY_USER event for signup: {} (likely Redis disabled or unavailable)", 
                    signUp.getId(), e);
        }
    }

    private void publishToStream(NotificationEvent event) {
        try {
            ObjectMapper objectMapper = ApplicationContextHolder.getBean(ObjectMapper.class);
            StringRedisTemplate redisTemplate = ApplicationContextHolder.getBean(StringRedisTemplate.class);
            
            if (objectMapper == null || redisTemplate == null) {
                log.debug("Redis or ObjectMapper bean not available, skipping event publishing");
                return;
            }
            
            String payload = objectMapper.writeValueAsString(event);
            redisTemplate.opsForStream().add(StreamConstants.NOTIFICATION_STREAM, Map.of("payload", payload));
        } catch (Exception e) {
            // If anything goes wrong getting beans or publishing, just log and continue
            // This is expected when Redis is disabled or unavailable
            log.debug("Could not publish to Redis stream (expected when Redis disabled)", e);
        }
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
