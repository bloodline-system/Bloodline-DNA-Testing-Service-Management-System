package com.dna_testing_system.dev.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceFeatureRequest {
    String featureName;
    Boolean isAvailable;
}
