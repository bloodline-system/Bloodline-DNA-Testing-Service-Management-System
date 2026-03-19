package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.NewReportRequest;
import com.dna_testing_system.dev.dto.request.UpdatingReportRequest;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.entity.SystemReport;

import java.util.List;

public interface SystemReportService {
    SystemReportResponse getSystemReportByUserGenerated(String userGeneratedId);
    SystemReportResponse getSystemReportByReportId(Long reportId);
    SystemReport getSystemReportEntityByReportId(Long reportId);
    List<SystemReportResponse> getAllSystemReports();
    List<SystemReportResponse> getAllSystemReportsByUserGenerated(String userGeneratedId);
    SystemReportResponse createNewReport(NewReportRequest newReportRequest);
    void updateExistReport(UpdatingReportRequest updatingReportRequest, Long systemReportId);
    void deleteExistingReport(Long existingReportId);
}
