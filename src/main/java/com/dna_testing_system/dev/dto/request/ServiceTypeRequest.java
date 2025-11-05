package com.dna_testing_system.dev.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceTypeRequest {
    @NotBlank(message = "Service type name is required")
    String typeName;
    @Builder.Default
    Boolean isActive = true;
}
