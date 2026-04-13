package com.dna_testing_system.dev.integration.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that provides shared MySQL and Redis Testcontainers.
 * 
 * This class sets up:
 * - MySQL database container for data persistence testing (shared across all test classes)
 * - Redis container for caching and event streaming
 * - Automatic flushRedis() cleanup between tests for reuse
 * 
 * All integration tests should extend this class to reuse the same containers
 * across multiple test classes, reducing startup time and resource consumption.
 */
@SpringBootTest(properties = {
    // Ensure test profile wins even if application.yaml sets spring.profiles.active via ENVIRONMENT_CONFIG
    "spring.profiles.active=test",
    // Redis is provided by the CI pipeline service container (localhost:6379)
    "app.redis.enabled=true",
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    // HikariCP connection pool settings for multi-test scenarios
    "spring.datasource.hikari.connection-test-query=SELECT 1",
    "spring.datasource.hikari.max-lifetime=600000",
    "spring.datasource.hikari.idle-timeout=60000",
    "spring.datasource.hikari.max-pool-size=10",
    "spring.datasource.hikari.connection-timeout=30000",
    "spring.datasource.hikari.validation-interval=5000",
    "spring.datasource.hikari.leak-detection-threshold=300000"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    // Shared container instance across all test classes
    static MySQLContainer<?> mysql = SharedTestContainer.getInstance();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL configuration
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        // Using create-drop can make Surefire hang on JVM shutdown due to long schema teardown
        // (foreign-key heavy schemas). Since the DB lives in a disposable container anyway,
        // `create` provides the same isolation with much faster shutdown.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Flushes all Redis data before each test method.
     * This ensures test isolation and allows Redis containers to be reused across tests.
     * Gracefully handles cases where Redis is not available or connection fails.
     */
    public void flushRedis() {
        try {
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return; // Redis not configured
            }
            
            var connection = connectionFactory.getConnection();
            if (connection != null) {
                try {
                    connection.flushAll();
                } finally {
                    connection.close();
                }
            }
        } catch (Exception e) {
            // Redis connection failed - treat as non-fatal for cleanup
        }
    }
}
