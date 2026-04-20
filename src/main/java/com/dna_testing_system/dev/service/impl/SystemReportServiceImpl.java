package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.NewReportRequest;
import com.dna_testing_system.dev.dto.request.UpdatingReportRequest;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.entity.SystemReport;
import com.dna_testing_system.dev.enums.ReportStatus;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.exception.ApplicationException;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.mapper.SystemReportMapper;
import com.dna_testing_system.dev.repository.RoleRepository;
import com.dna_testing_system.dev.repository.SystemReportRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.SystemReportService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SystemReportServiceImpl implements SystemReportService {
    SystemReportRepository systemReportRepository;
    UserRepository userRepository;
    SystemReportMapper systemReportMapper;
    private final RoleRepository roleRepository;

    @Override
    public SystemReportResponse getSystemReportByUserGenerated(String userGeneratedId) {
        var user =  userRepository.findById(userGeneratedId)
                .orElseThrow(()-> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));
        var existingReport = systemReportRepository.findByGeneratedByUser(user)
                .orElseThrow(()-> new EntityNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
        return systemReportMapper.toResponse(existingReport);
    }

    @Override
    public SystemReportResponse getSystemReportByReportId(Long reportId) {
        var existingReport = systemReportRepository.findById(reportId)
                .orElseThrow(()-> new EntityNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
        return systemReportMapper.toResponse(existingReport);
    }

    @Override
    public SystemReport getSystemReportEntityByReportId(Long reportId) {
        return systemReportRepository.findById(reportId)
                .orElseThrow(()-> new EntityNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Override
    public List<SystemReportResponse> getAllSystemReports() {
        List<SystemReport> existingReports = systemReportRepository.findAll();
        if (existingReports.isEmpty()) {
            return Collections.emptyList();
        }
        return existingReports.stream()
                .map(systemReportMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SystemReportResponse> getAllSystemReportsByUserGenerated(String userGeneratedId) {
        List<SystemReport> existingReports = systemReportRepository.findAllByGeneratedByUser_Id(userGeneratedId);
        if (existingReports.isEmpty()) {
            return Collections.emptyList();
        }
        return existingReports.stream()
                .map(systemReportMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SystemReportResponse createNewReport(NewReportRequest newReportRequest) {
        try {
            var systemReport = systemReportRepository.save(systemReportMapper.toEntity(newReportRequest));
            return systemReportMapper.toResponse(systemReport);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApplicationException(ErrorCode.MANIPULATION_SYSTEM_REPORT_FAILED);
        }
    }

    @Override
    @Transactional
    public void updateExistReport(UpdatingReportRequest updatingReportRequest, Long systemReportId) {
        ReportStatus reportStatus;
        try {
            reportStatus = ReportStatus.valueOf(updatingReportRequest.getNewReportStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApplicationException(ErrorCode.VALIDATE_ENUMERATION_FAILED, "Parsing status is not valid");
        }
        var existingReport = systemReportRepository.findById(systemReportId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
        var editingByUser = userRepository.findById(updatingReportRequest.getGeneratedByUserId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_EXISTS));
        boolean isStaff = editingByUser.getUserRoles()
                .stream().anyMatch(role -> role.getRole().getRoleName().equals(RoleType.STAFF.name()));
        boolean isCustomer = editingByUser.getUserRoles()
                .stream().anyMatch(role -> role.getRole().getRoleName().equals(RoleType.CUSTOMER.name()));
        if (!existingReport.getReportStatus().equals(reportStatus) && (isStaff || isCustomer)) {
            throw new ApplicationException(ErrorCode.MANIPULATION_SYSTEM_REPORT_FAILED, "No changing status allowed");
        }
        if (!existingReport.getGeneratedByUser().getId().equals(editingByUser.getId()) && (isStaff || isCustomer)) {
            throw new ApplicationException(ErrorCode.MANIPULATION_SYSTEM_REPORT_FAILED, "No editing allowed");
        }
        existingReport.setReportStatus(reportStatus);
        try {
            systemReportMapper.updateEntity(existingReport, updatingReportRequest);
            existingReport.setGeneratedByUser(editingByUser);
            systemReportRepository.save(existingReport);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApplicationException(ErrorCode.MANIPULATION_SYSTEM_REPORT_FAILED);
        }

    }

    @Override
    @Transactional
    public void deleteExistingReport(Long existingReportId) {
        try {
            systemReportRepository.deleteById(existingReportId);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new ApplicationException(ErrorCode.MANIPULATION_SYSTEM_REPORT_FAILED);
        }
    }
}
