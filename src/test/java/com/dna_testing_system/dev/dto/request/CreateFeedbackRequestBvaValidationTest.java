package com.dna_testing_system.dev.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateFeedbackRequestBvaValidationTest {

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
    void requiredFieldsPresentAndBoundariesValid_isValid() {
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .serviceId(1L)
                .customerId("u1")
                .serviceQualityRating(1)
                .staffBehaviorRating(5)
                .timelinessRating(3)
                .feedbackTitle("t".repeat(255))
                .build();

        var violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void missingServiceId_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setServiceId(null);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("serviceId");
    }

    @Test
    void missingCustomerId_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setCustomerId(null);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("customerId");
    }

    @Test
    void serviceQualityRatingBelowMin_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setServiceQualityRating(0);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("serviceQualityRating");
    }

    @Test
    void serviceQualityRatingAboveMax_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setServiceQualityRating(6);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("serviceQualityRating");
    }

    @Test
    void staffBehaviorRatingBelowMin_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setStaffBehaviorRating(0);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("staffBehaviorRating");
    }

    @Test
    void staffBehaviorRatingAboveMax_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setStaffBehaviorRating(6);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("staffBehaviorRating");
    }

    @Test
    void timelinessRatingBelowMin_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setTimelinessRating(0);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("timelinessRating");
    }

    @Test
    void timelinessRatingAboveMax_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setTimelinessRating(6);

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("timelinessRating");
    }

    @Test
    void feedbackTitleLength256_isInvalid() {
        CreateFeedbackRequest request = validRequest();
        request.setFeedbackTitle("x".repeat(256));

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("feedbackTitle");
    }

    private CreateFeedbackRequest validRequest() {
        return CreateFeedbackRequest.builder()
                .serviceId(1L)
                .customerId("u1")
                .serviceQualityRating(3)
                .staffBehaviorRating(3)
                .timelinessRating(3)
                .feedbackTitle("Good")
                .build();
    }
}
