package com.dna_testing_system.dev.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileRequestBvaValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void firstNameLength100_isValid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .firstName("a".repeat(100))
                .email("alice@example.com")
                .build();

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void firstNameLength101_isInvalid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .firstName("a".repeat(101))
                .email("alice@example.com")
                .build();

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("firstName");
    }

    @Test
    void lastNameLength100_isValid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .lastName("b".repeat(100))
                .email("alice@example.com")
                .build();

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void lastNameLength101_isInvalid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .lastName("b".repeat(101))
                .email("alice@example.com")
                .build();

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("lastName");
    }

    @Test
    void phoneLength20_isValid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .phoneNumber("1".repeat(20))
                .email("alice@example.com")
                .build();

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void phoneLength21_isInvalid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .phoneNumber("1".repeat(21))
                .email("alice@example.com")
                .build();

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("phoneNumber");
    }

    @Test
    void phonePatternInvalid_isInvalid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .phoneNumber("123abc")
                .email("alice@example.com")
                .build();

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("phoneNumber");
    }

    @Test
    void emailNull_isInvalid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .firstName("Alice")
                .build();

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("email");
    }

    @Test
    void emailInvalidFormat_isInvalid() {
        UserProfileRequest request = UserProfileRequest.builder()
                .email("invalid-email")
                .build();

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("email");
    }
}
