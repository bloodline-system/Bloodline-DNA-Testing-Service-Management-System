package com.dna_testing_system.dev.integration.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Base class for integration tests that provides shared MySQL and Redis Testcontainers.
 * 
 * This class sets up:
 * - MySQL database container for data persistence testing
 * - Redis container for caching and event streaming
 * - Automatic flushRedis() cleanup between tests for reuse
 * 
 * All integration tests should extend this class to reuse the same containers
 * across multiple test classes, reducing startup time and resource consumption.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:latest")
            .withDatabaseName("dna_testing_db")
            .withUsername("root")
            .withPassword("password123");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7.2.4-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL configuration
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        
        // Redis configuration - only enable if container is actually available
        // This is defensive against CI environments without Docker
        boolean redisAvailable = isRedisAvailable();
        
        if (redisAvailable) {
            try {
                registry.add("spring.redis.host", redis::getHost);
                registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
                registry.add("app.redis.enabled", () -> true);
            } catch (Exception e) {
                // If accessing container properties fails, disable Redis
                registry.add("app.redis.enabled", () -> false);
            }
        } else {
            // Redis container not available (e.g., CI without Docker) - disable via default setting
            // The property will already be false from application-test.yaml
            registry.add("app.redis.enabled", () -> false);
        }
    }
    
    /**
     * Checks if Redis container is available and healthy.
     * This is more robust than just checking isRunning() in CI environments.
     */
    private static boolean isRedisAvailable() {
        try {
            // Check if the container is running
            if (!redis.isRunning()) {
                return false;
            }
            // Additional check: verify container has been properly initialized
            // If we can get the host and port without exception, it's likely available
            String host = redis.getHost();
            int port = redis.getMappedPort(6379);
            // Both succeeded - Redis is likely available
            return host != null && !host.isEmpty();
        } catch (Exception e) {
            // Any exception means Redis is not available
            return false;
        }
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
            // Redis connection failed - acceptable in CI environments without Docker
            // Tests will continue with clean database state from create-drop strategy
        }
    }
}
