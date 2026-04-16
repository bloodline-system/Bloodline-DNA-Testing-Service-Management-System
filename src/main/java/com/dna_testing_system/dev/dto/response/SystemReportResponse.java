package com.dna_testing_system.dev.dto.response;

import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.ReportType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SystemReportResponse {
    Long reportId;
    String reportName;
    ReportType reportType;
    String reportCategory;
    String generatedByUserImageUrl;
    String generatedByUserEmail;
    String generatedByUserRole;
    String generatedByUserName;
    String reportData;
    ReportStatus reportStatus;
}
