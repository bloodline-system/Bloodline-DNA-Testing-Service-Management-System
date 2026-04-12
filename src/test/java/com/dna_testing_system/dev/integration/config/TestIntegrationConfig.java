package com.dna_testing_system.dev.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test-specific configuration for integration tests.
 * 
 * This configuration is loaded in addition to the main application configuration
 * and provides test overrides for certain components that may not be available
 * in CI environments (like when Docker is not available).
 */
@TestConfiguration
public class TestIntegrationConfig {
    
    /**
     * Provides a safe no-op StreamMessageListenerContainer bean for tests.
     * This prevents errors when Redis is not available but the application
     * tries to create stream listeners.
     * 
     * The application's own RedisStreamConfig bean will not be created
     * due to @ConditionalOnProperty when app.redis.enabled=false,
     * but this ensures a fallback behavior if needed.
     */
    @Bean(name = "testMailStreamListener")
    public Object testMailStreamListener() {
        // This bean exists but is not actually used since RedisStreamConfig
        // won't be created when Redis is disabled
        return new Object();
    }
}
