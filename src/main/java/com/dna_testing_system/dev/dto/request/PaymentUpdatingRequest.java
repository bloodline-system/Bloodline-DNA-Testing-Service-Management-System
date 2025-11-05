package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentUpdatingRequest {
    Long paymentId;
    String paymentStatus;
}
