package com.dna_testing_system.dev.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    Long id;
    Long orderId;
    String serviceName;
    String customerName;
    String email;
    String phoneNumber;
    String collectionAddress;
    String promotionCode;
    String paymentMethod;
    BigDecimal grossAmount;
    BigDecimal discountAmount = BigDecimal.ZERO;
    BigDecimal netAmount;
    String paymentStatus;
    LocalDateTime paymentDate;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
