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

            try {
                publishToStream(event);
                log.info("Published VERIFY_USER event for signup: {}", signUp.getId());
            } catch (Exception redisError) {
                // Redis connection failed - acceptable in test environments without Docker
                log.warn("Could not publish VERIFY_USER event to Redis for signup: {} (Redis may be unavailable)", 
                        signUp.getId(), redisError);
            }
        } catch (Exception e) {
            log.error("Failed to build VERIFY_USER event for signup: {}", signUp.getId(), e);
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
     */
    private boolean isRedisEnabled() {
        try {
            Environment environment = ApplicationContextHolder.getBean(Environment.class);
            return environment.getProperty("app.redis.enabled", Boolean.class, true);
        } catch (Exception e) {
            log.warn("Unable to determine Redis enabled status, defaulting to true", e);
            return true;
        }
    }
}
