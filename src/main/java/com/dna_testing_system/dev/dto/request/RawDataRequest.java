package com.dna_testing_system.dev.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RawDataRequest {
    String dataFormat;
    String rawDataContent;
    String file;
    LocalDateTime collectionDate;
}
