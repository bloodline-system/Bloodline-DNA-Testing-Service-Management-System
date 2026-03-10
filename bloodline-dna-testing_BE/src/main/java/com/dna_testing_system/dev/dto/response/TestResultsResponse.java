package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.enums.ResultStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestResultsResponse {
    Long testResultId;
    Long orderId;
    Long rawTestId;
    String staff;
    LocalDateTime testDate;
    String resultSummary;
    String detailedResults;
    String reportFile;
    ResultStatus resultStatus;
    Boolean reportGenerated;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
