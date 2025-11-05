package com.dna_testing_system.dev.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RawDataResponse {
    Long rawDataId;
    String dataFormat;
    String rawDataContent;
    String filePath;
    LocalDateTime collectedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
