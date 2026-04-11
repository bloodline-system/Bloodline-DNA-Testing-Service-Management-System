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
import org.testcontainers.lifecycle.Startables;
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
@SpringBootTest(properties = {
    // Ensure test profile wins even if application.yaml sets spring.profiles.active via ENVIRONMENT_CONFIG
    "spring.profiles.active=test"
})
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
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
        // Testcontainers are started by the JUnit extension, but DynamicPropertySource may be evaluated
        // before that happens. Start containers here to deterministically register mapped ports.
        Startables.deepStart(mysql, redis).join();

        // MySQL configuration
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        // Using create-drop can make Surefire hang on JVM shutdown due to long schema teardown
        // (foreign-key heavy schemas). Since the DB lives in a disposable container anyway,
        // `create` provides the same isolation with much faster shutdown.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");

        // Redis configuration
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.redis.enabled", () -> true);
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
