package com.dna_testing_system.dev.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/**
 * Composite response for GET /api/v1/orders/{id}
 * Combines order info, kits, participants and payment total.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetailResponse {

    ServiceOrderByCustomerResponse order;
    List<OrderTestKitResponse> kits;
    List<OrderParticipantResponse> participants;
    BigDecimal paymentTotal;
}