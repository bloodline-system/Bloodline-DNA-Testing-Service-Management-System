package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.entity.*;
import com.dna_testing_system.dev.enums.CollectionType;
import com.dna_testing_system.dev.enums.PaymentStatus;
import com.dna_testing_system.dev.enums.ServiceCategory;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceOrderResponse {
    Long id;
    String customerName;
    String username;
    String profileImageUrl;
    String email;
    String phoneNumber;
    String serviceName;
    String serviceType;
    String serviceCategory;
    Integer participants;
    Integer executionTimeDays;
    LocalDateTime appointmentDate;
    CollectionType collectionType;
    String collectionAddress;
    ServiceOrderStatus orderStatus = ServiceOrderStatus.PENDING;
    PaymentStatus paymentStatus = PaymentStatus.PENDING;
    BigDecimal totalAmount;
    BigDecimal discountAmount;
    BigDecimal finalAmount;
    LocalDateTime createdAt;
    String collectionStaffName;
    String collectionStaffEmail;
    String collectionStaffPhone;
    String analysisStaffName;
    String analysisStaffEmail;
    String analysisStaffPhone;
    String testingResultStatus;
    String testingResultSummary;
    String detailedResults;
    RawTestData rawData;
    String testingResultAnalyzedBy;
    LocalDateTime testingResultAnalysisDate;
    Set<OrderKit> orderKits;
    Set<OrderParticipant> orderParticipants;
    Set<SampleCollection> sampleCollections;
}
