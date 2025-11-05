package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CRUDorderResponse {
    Long idServiceOrder;
    String userName;
    String userEmail;
    String userPhoneNumber;
    String medicalServiceName;
    LocalDateTime appointmentDate;
    String collectionType;
    String collectionAddress;
    BigDecimal finalAmount;
    ServiceOrderStatus orderStatus;
    LocalDateTime createdAt;
}
