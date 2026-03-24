package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.UpdatingReportRequest;
import com.dna_testing_system.dev.entity.SystemReport;
import com.dna_testing_system.dev.enums.ReportType;
import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.exception.ApplicationException;
import com.dna_testing_system.dev.mapper.SystemReportMapper;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.SystemReportRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.impl.SystemReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SystemReportServiceImplTest {

    @Mock
    private SystemReportRepository systemReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SystemReportMapper systemReportMapper;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private SystemReportServiceImpl reportService;

    @Test
    void updateExistReport_whenStatusIsInvalid_throwsApplicationException() {
        UpdatingReportRequest request = UpdatingReportRequest.builder()
                .reportName("Test Report")
                .reportType(ReportType.DAILY_ORDERS)
                .reportCategory("Test")
                .generatedByUserId("user-1")
                .reportData("payload")
                .newReportStatus("INVALID_STATUS")
                .build();

        assertThrows(ApplicationException.class, () -> reportService.updateExistReport(request, 100L));
    }
}
