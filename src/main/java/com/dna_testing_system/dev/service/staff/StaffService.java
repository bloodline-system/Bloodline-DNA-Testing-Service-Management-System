package com.dna_testing_system.dev.service.staff;

import com.dna_testing_system.dev.dto.request.staff.RawDataRequest;
import com.dna_testing_system.dev.dto.request.staff.TestResultsResquest;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.dto.response.staff.CRUDsampleCollectionResponse;
import com.dna_testing_system.dev.dto.response.staff.TestResultsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StaffService {
    List<CRUDorderResponse> getForStaff(String username);
    CRUDorderResponse getOrderById(Long orderId);
    List<CRUDsampleCollectionResponse> getSampleCollectionTasks(String username);
    CRUDsampleCollectionResponse getSampleCollectionTasksById(Long sampleCollectionId);
    void updateSampleCollectionStatus(Long sampleCollectionId, String collectionStatus, String sampleQuality);
    List<TestResultsResponse> getTestResults(String username);
    TestResultsResponse getTestResultById(Long testResultId);
    void updateTestResult(TestResultsResquest testResultsResquest, Long testResultId);
    List<RawDataResponse> getRawData(String username);
    RawDataResponse getRawDataById(Long rawDataId);
    void updateRawData(RawDataRequest rawDataRequest, Long rawDataId);
    void createRawData(RawDataRequest rawDataRequest, Long testResultId);
    List<CRUDorderResponse> filterOrdersByStatus(String status, String username);
    List<CRUDsampleCollectionResponse> filterSampleCollectionsByStatus(String status, String username);
    List<TestResultsResponse> filterTestResultsByStatus(String status, String username);
    CRUDorderResponse searchOrderById(Long orderId, String username);
    CRUDsampleCollectionResponse searchSampleCollectionById(Long sampleCollectionId, String username);
    TestResultsResponse searchTestResultById(Long testResultId, String username);

    // Pageable variants for REST API
    Page<CRUDorderResponse> getOrdersPage(String username, String status, Pageable pageable);
    Page<CRUDsampleCollectionResponse> getSampleCollectionsPage(String username, String status, Pageable pageable);
    Page<TestResultsResponse> getTestResultsPage(String username, String status, Pageable pageable);
}
