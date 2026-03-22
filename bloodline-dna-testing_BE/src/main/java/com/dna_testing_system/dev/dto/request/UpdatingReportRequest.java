package com.dna_testing_system.dev.dto.request;

import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.ReportType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdatingReportRequest {
    @Size(max = 255)
    @NotNull
    String reportName;

    @Enumerated(EnumType.STRING)
    @NotNull
    ReportType reportType;

    @Size(max = 100)
    @NotNull
    String reportCategory;

    @NotNull
    String generatedByUserId;

    @NotNull
    String reportData;

    @Size(max = 1000)
    String reportFilePath;

    String newReportStatus;
}
