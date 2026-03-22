package com.dna_testing_system.dev.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FeatureAssignmentCreationRequest {
    @NotNull(message = "Feature ID must not be null")
    String featureName;
    
    @NotNull(message = "Availability status must be specified")
    Boolean isAvailable;
}
