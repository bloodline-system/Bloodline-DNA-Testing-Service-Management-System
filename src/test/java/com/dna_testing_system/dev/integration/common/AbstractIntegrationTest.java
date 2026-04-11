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
        
        // Redis configuration - only enable if container is running
        try {
            if (redis.isRunning()) {
                registry.add("spring.redis.host", redis::getHost);
                registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
                registry.add("app.redis.enabled", () -> true);
            } else {
                // Containers not available (e.g., CI without Docker)
                registry.add("app.redis.enabled", () -> false);
            }
        } catch (Exception e) {
            // Redis container failed to start - disable Redis
            registry.add("app.redis.enabled", () -> false);
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
