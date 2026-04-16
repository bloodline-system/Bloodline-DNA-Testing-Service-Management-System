package com.dna_testing_system.dev.dto.request;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderTestKitRequest {
    Long orderId;
    Long kitTestId;
    Integer quantityOrdered;

}
