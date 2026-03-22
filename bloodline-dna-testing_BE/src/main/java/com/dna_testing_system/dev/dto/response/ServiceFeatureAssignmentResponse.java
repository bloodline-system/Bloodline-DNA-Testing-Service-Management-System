package com.dna_testing_system.dev.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO for returning information about a feature assigned to a service
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceFeatureAssignmentResponse {

    Long featureId;
    String featureName;
    Boolean isAvailable;

}
