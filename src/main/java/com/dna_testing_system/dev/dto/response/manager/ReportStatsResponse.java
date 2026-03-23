package com.dna_testing_system.dev.dto.response.manager;

import com.dna_testing_system.dev.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReportStatsResponse {
    private long totalReports;
    private long generatedReports;
    private long approvedReports;
    private long rejectedReports;
    private Map<ReportType, Long> reportsByType;
}
