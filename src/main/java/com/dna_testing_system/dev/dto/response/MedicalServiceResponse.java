package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.enums.ServiceCategory;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalServiceResponse {

    Long id;
    String serviceName;
    ServiceCategory serviceCategory;
    Long serviceTypeId;
    String serviceTypeName;
    Integer participants;
    Integer executionTimeDays;
    BigDecimal basePrice;
    BigDecimal currentPrice;
    Boolean isAvailable;
    String serviceDescription;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime updatedAt;
    List<ServiceFeatureAssignmentResponse> features;

}
