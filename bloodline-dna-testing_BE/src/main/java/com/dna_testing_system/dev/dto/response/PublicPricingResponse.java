package com.dna_testing_system.dev.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicPricingResponse {
    List<MedicalServiceResponse> individualServices;
    List<MedicalServiceResponse> clinicalServices;
}
