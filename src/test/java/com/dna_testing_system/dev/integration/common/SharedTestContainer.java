package com.dna_testing_system.dev.integration.common;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.lifecycle.Startables;

/**
 * Singleton container for shared MySQL instance across all integration tests.
 *
 * This ensures that:
 * 1. Only ONE MySQL container is created and started for the entire test suite
 * 2. The container persists across multiple test classes
 * 3. The container lifecycle is managed centrally and not recreated per test class
 *
 * Testcontainers @Container annotation creates issues when multiple test classes
 * use it, as the JUnit extension tries to manage lifecycle per class. This singleton
 * pattern bypasses that and uses explicit lifecycle management instead.
 */
public class SharedTestContainer {

    private static MySQLContainer<?> instance;
    private static final Object LOCK = new Object();

    private SharedTestContainer() {
        // Prevent instantiation
    }

    /**
     * Get the shared MySQL container instance.
     * Lazily initializes and starts the container on first access.
     */
    public static MySQLContainer<?> getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new MySQLContainer<>("mysql:latest")
                            .withDatabaseName("dna_testing_db")
                            .withUsername("root")
                            .withPassword("password123");

                    // Start the container
                    try {
                        Startables.deepStart(instance).join();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to start MySQL container", e);
                    }
                }
            }
        }
        return instance;
    }
}
