package com.dna_testing_system.dev.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTestKitResponse {
    Long id;
    String kitName;
    String kitType;
    String sampleType;
    Integer quantityOrdered;
    BigDecimal unitPrice;
    BigDecimal totalPrice;
}
