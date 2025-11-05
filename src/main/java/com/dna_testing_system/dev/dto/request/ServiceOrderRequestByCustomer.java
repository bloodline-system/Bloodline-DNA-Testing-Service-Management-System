package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.enums.CollectionType;
import com.dna_testing_system.dev.enums.PaymentMethod;
import com.dna_testing_system.dev.enums.PaymentMethod;
import com.dna_testing_system.dev.enums.PaymentStatus;
import com.dna_testing_system.dev.enums.PaymentMethod;
import com.dna_testing_system.dev.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceOrderRequestByCustomer {
    String username;
    Long idMedicalService;
    LocalDateTime appointmentDate;
    CollectionType collectionType;
    String collectionAddress;
    PaymentMethod paymentMethod;
    String createdBy;
    String idKit;
    Integer quantityKit;
    List<ParticipantRequest> participants;
    Long promotionId;
    PaymentStatus paymentStatus;
}
