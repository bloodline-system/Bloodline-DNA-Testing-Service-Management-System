package com.dna_testing_system.dev.dto.request.staff;

import jakarta.validation.constraints.NotBlank;

public record SampleCollectionStatusUpdateRequest(
        @NotBlank(message = "Collection status must not be blank")
        String collectionStatus,

        @NotBlank(message = "Sample quality must not be blank")
        String sampleQuality
) {}
