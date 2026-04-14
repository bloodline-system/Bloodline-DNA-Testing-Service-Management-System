# Redis Testcontainers for Integration Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure integration tests spin up Redis via Testcontainers while non-integration tests run with Redis disabled, eliminating CI failures.

**Architecture:** Add a shared integration test base class that owns MySQL + Redis Testcontainers and DynamicPropertySource wiring. Guard Redis stream consumer and entity listener publishing behind app.redis.enabled, defaulting true in production and false in the test profile.

**Tech Stack:** Spring Boot 4, JUnit 5, Testcontainers, MySQL, Redis

---

## File Structure

**Create**
- src/test/java/com/dna_testing_system/dev/integration/support/AbstractIntegrationTest.java
- src/test/java/com/dna_testing_system/dev/redis/RedisPublishingDisabledTest.java

**Modify**
- src/main/resources/application.yaml
- src/main/resources/application-test.yaml
- src/main/java/com/dna_testing_system/dev/config/RedisStreamConfig.java
- src/main/java/com/dna_testing_system/dev/event/listener/UserEntityListener.java
- src/main/java/com/dna_testing_system/dev/event/listener/SignUpEntityListener.java
- src/test/java/com/dna_testing_system/dev/integration/auth/AuthIntegrationTest.java
- src/test/java/com/dna_testing_system/dev/DevApplicationTests.java

---

### Task 0: Prepare a Dedicated Worktree (if not already in one)

**Files:** none

- [ ] **Step 1: Create worktree**

Run:
```
git worktree add ..\bloodline-dna-testing_BE-redis-it HEAD
```
Expected: new worktree created without errors.

- [ ] **Step 2: Switch to worktree**

Run:
```
cd ..\bloodline-dna-testing_BE-redis-it
```
Expected: working directory is the new worktree.

---

### Task 1: Add Failing Test for Redis Disabled in Tests

**Files:**
- Create: src/test/java/com/dna_testing_system/dev/redis/RedisPublishingDisabledTest.java
- Modify: src/test/java/com/dna_testing_system/dev/DevApplicationTests.java

- [ ] **Step 1: Write the failing test**

Create src/test/java/com/dna_testing_system/dev/redis/RedisPublishingDisabledTest.java:
```java
package com.dna_testing_system.dev.redis;

import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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
```

- [ ] **Step 2: Align the context-load test to test profile**

Update src/test/java/com/dna_testing_system/dev/DevApplicationTests.java:
```java
package com.dna_testing_system.dev;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DevApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 3: Run the test to confirm it fails**

Run:
```
mvn -Dtest=RedisPublishingDisabledTest test
```
Expected: FAIL with Redis connection attempt (context init fails) because app.redis.enabled is not wired yet.

---

### Task 2: Add Redis Enable Flag and Conditional Stream Config

**Files:**
- Modify: src/main/resources/application.yaml
- Modify: src/main/resources/application-test.yaml
- Modify: src/main/java/com/dna_testing_system/dev/config/RedisStreamConfig.java

- [ ] **Step 1: Add app.redis.enabled to production config**

Update src/main/resources/application.yaml:
```yaml
app:
  redis:
    enabled: true
```

- [ ] **Step 2: Disable Redis by default in test profile**

Update src/main/resources/application-test.yaml:
```yaml
app:
  redis:
    enabled: false
```

- [ ] **Step 3: Guard Redis stream listener with conditional property**

Update src/main/java/com/dna_testing_system/dev/config/RedisStreamConfig.java:
```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class RedisStreamConfig {
    // existing code
}
```

- [ ] **Step 4: Run the test to confirm it passes**

Run:
```
mvn -Dtest=RedisPublishingDisabledTest test
```
Expected: PASS (context loads without Redis connections).

- [ ] **Step 5: Commit**

Run:
```
git add src/main/resources/application.yaml src/main/resources/application-test.yaml src/main/java/com/dna_testing_system/dev/config/RedisStreamConfig.java src/test/java/com/dna_testing_system/dev/DevApplicationTests.java src/test/java/com/dna_testing_system/dev/redis/RedisPublishingDisabledTest.java

git commit -m "test: disable redis stream in test profile"
```

---

### Task 3: Skip Redis Publishing in Entity Listeners When Disabled

**Files:**
- Modify: src/main/java/com/dna_testing_system/dev/event/listener/UserEntityListener.java
- Modify: src/main/java/com/dna_testing_system/dev/event/listener/SignUpEntityListener.java

- [ ] **Step 1: Add redis-enabled check in UserEntityListener**

Update src/main/java/com/dna_testing_system/dev/event/listener/UserEntityListener.java:
```java
import org.springframework.core.env.Environment;

private boolean isRedisEnabled() {
    Environment environment = ApplicationContextHolder.getBean(Environment.class);
    return environment.getProperty("app.redis.enabled", Boolean.class, true);
}

@PostPersist
public void onUserCreated(User user) {
    if (!isRedisEnabled()) {
        return;
    }
    // existing code
}
```

- [ ] **Step 2: Add redis-enabled check in SignUpEntityListener**

Update src/main/java/com/dna_testing_system/dev/event/listener/SignUpEntityListener.java:
```java
import org.springframework.core.env.Environment;

private boolean isRedisEnabled() {
    Environment environment = ApplicationContextHolder.getBean(Environment.class);
    return environment.getProperty("app.redis.enabled", Boolean.class, true);
}

@PostPersist
public void onSignUpCreated(SignUp signUp) {
    if (!isRedisEnabled()) {
        return;
    }
    publishVerifyUserEvent(signUp);
}

@PostUpdate
public void onSignUpUpdated(SignUp signUp) {
    if (!isRedisEnabled()) {
        return;
    }
    if (SignUpStatus.PENDING.equals(signUp.getStatus())) {
        publishVerifyUserEvent(signUp);
    }
}
```

- [ ] **Step 3: Run the test to confirm it still passes**

Run:
```
mvn -Dtest=RedisPublishingDisabledTest test
```
Expected: PASS.

- [ ] **Step 4: Commit**

Run:
```
git add src/main/java/com/dna_testing_system/dev/event/listener/UserEntityListener.java src/main/java/com/dna_testing_system/dev/event/listener/SignUpEntityListener.java

git commit -m "fix: skip redis publishing when disabled"
```

---

### Task 4: Add Integration Base Class with MySQL + Redis Containers

**Files:**
- Create: src/test/java/com/dna_testing_system/dev/integration/support/AbstractIntegrationTest.java

- [ ] **Step 1: Create the base class**

Create src/test/java/com/dna_testing_system/dev/integration/support/AbstractIntegrationTest.java:

```java
package com.dna_testing_system.dev.integration.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.wait.strategy.Wait;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@AutoConfigureMockMvc
@Execution(ExecutionMode.SAME_THREAD)
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
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.redis.enabled", () -> true);
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }
}
```

- [ ] **Step 2: Commit**

Run:
```
git add src/test/java/com/dna_testing_system/dev/integration/support/AbstractIntegrationTest.java

git commit -m "test: add integration base class with mysql+redis"
```

---

### Task 5: Update AuthIntegrationTest to Use Base Class

**Files:**
- Modify: src/test/java/com/dna_testing_system/dev/integration/auth/AuthIntegrationTest.java

- [ ] **Step 1: Extend the base class and remove per-class containers**

Update src/test/java/com/dna_testing_system/dev/integration/auth/AuthIntegrationTest.java:

```java
import com.dna_testing_system.dev.integration.common.AbstractIntegrationTest;

class AuthIntegrationTest extends AbstractIntegrationTest {
    // remove MySQLContainer field, @Testcontainers, @DynamicPropertySource, and @ActiveProfiles
}
```

- [ ] **Step 2: Run the integration test**

Run:
```
mvn -Dtest=AuthIntegrationTest test
```
Expected: PASS (requires Docker).

- [ ] **Step 3: Commit**

Run:
```
git add src/test/java/com/dna_testing_system/dev/integration/auth/AuthIntegrationTest.java

git commit -m "test: use integration base class for auth"
```

---

### Task 6: Final Verification

**Files:** none

- [ ] **Step 1: Run targeted tests**

Run:
```
mvn -Dtest=RedisPublishingDisabledTest,DevApplicationTests test
```
Expected: PASS.

- [ ] **Step 2: Optional full test run**

Run:
```
mvn test
```
Expected: PASS (requires Docker for integration tests).
