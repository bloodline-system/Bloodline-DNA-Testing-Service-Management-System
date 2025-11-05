package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.enums.ResultStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestResultsResquest {
    Long rawDataId;
    LocalDateTime testDate;
    String resultSummary;
    String detailedResults;
    String reportFile;
    Boolean reportGenerated;
    ResultStatus resultStatus;
}
