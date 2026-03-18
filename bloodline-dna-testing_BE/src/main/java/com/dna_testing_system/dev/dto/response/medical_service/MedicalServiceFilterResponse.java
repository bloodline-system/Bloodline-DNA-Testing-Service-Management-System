package com.dna_testing_system.dev.dto.response.medical_service;

import com.dna_testing_system.dev.enums.ServiceCategory;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalServiceFilterResponse {
    Long id;
    String serviceName;
    ServiceCategory serviceCategory;
    String serviceTypeName;
    Integer participants;
    BigDecimal currentPrice;
    Boolean isAvailable;
    String serviceDescription;
}
