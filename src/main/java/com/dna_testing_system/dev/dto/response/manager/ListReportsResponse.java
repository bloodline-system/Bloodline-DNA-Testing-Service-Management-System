package com.dna_testing_system.dev.dto.response.manager;

import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.ReportType;
import com.dna_testing_system.dev.enums.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ListReportsResponse {
    private List<SystemReportResponse> reports;
    private ReportStatsResponse reportStats;
    private Map<String, Object> pagination;
    private Map<String, Object> filters;
    private RoleType[] allRoles;
    private ReportStatus[] reportStatuses;
    private ReportType[] reportTypes;
}
