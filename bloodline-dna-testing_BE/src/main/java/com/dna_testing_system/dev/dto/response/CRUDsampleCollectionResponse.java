package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.enums.CollectionStatus;
import com.dna_testing_system.dev.enums.SampleQuality;
import com.dna_testing_system.dev.enums.SampleType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CRUDsampleCollectionResponse {
    Long collectionId;
    ServiceOrder order;
    LocalDateTime collectionDate;
    SampleQuality sampleQuality;
    CollectionStatus collectionStatus;
    SampleType sampleType;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime assignAt;
}
