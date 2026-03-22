package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.enums.ServiceCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalServiceUpdateRequest {
    @NotBlank(message = "Service name cannot be blank")
    @Size(max = 255, message = "Service name must be less than 255 characters")
    String serviceName;

    @NotNull(message = "Service category must not be null")
    ServiceCategory serviceCategory;

    @NotNull(message = "Service type ID must not be null")
    Long serviceTypeId;

    @NotNull(message = "Number of participants must not be null")
    @Min(value = 1, message = "At least one participant is required")
    Integer participants;

    @NotNull(message = "Execution time must not be null")
    @Min(value = 1, message = "Execution time must be at least 1 day")
    Integer executionTimeDays;

    @NotNull(message = "Base price must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than zero")
    BigDecimal basePrice;

    @NotNull(message = "Current price must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Current price must be greater than zero")
    BigDecimal currentPrice;

    @NotNull(message = "Availability status must be specified")
    Boolean isAvailable;

    @NotBlank(message = "Service description cannot be blank")
    String serviceDescription;

    @NotEmpty(message = "Feature assignments cannot be empty")
    @Valid
    List<FeatureAssignmentCreationRequest> editFeatureAssignments;
}
