package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.enums.SampleQuality;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SampleCollectionResponse {
    private Long id;
    private String sampleId;
    private LocalDateTime collectionDateTime;
    private String collectedBy;
    private String sampleType;
    private SampleQuality sampleQuality;
    private String conditionNotes;

}