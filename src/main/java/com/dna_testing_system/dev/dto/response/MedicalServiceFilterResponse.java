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
