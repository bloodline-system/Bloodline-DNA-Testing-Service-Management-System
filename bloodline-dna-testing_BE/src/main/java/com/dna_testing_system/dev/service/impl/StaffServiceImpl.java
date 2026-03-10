package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.RawDataRequest;
import com.dna_testing_system.dev.dto.request.TestResultsResquest;
import com.dna_testing_system.dev.dto.response.*;
import com.dna_testing_system.dev.entity.RawTestData;
import com.dna_testing_system.dev.entity.SampleCollection;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.enums.CollectionStatus;
import com.dna_testing_system.dev.enums.SampleQuality;
import com.dna_testing_system.dev.mapper.*;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.RawTestDataRepository;
import com.dna_testing_system.dev.repository.SampleCollectionRepository;
import com.dna_testing_system.dev.repository.TestResultRepository;
import com.dna_testing_system.dev.service.OrderService;
import com.dna_testing_system.dev.service.StaffService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StaffServiceImpl implements StaffService {

    TestResultRepository testResultRepository;
    OrderServiceRepository orderServiceRepository;
    CRUDorderMapper orderServiceMapper;
    SampleCollectionRepository sampleCollectionRepository;
    CRUDsampleCollectionMapper sampleCollectionMapper;
    TestResultsMapper testResultsMapper;
    RawTestDataRepository rawTestDataRepository;
    RawDataMapper rawDataMapper;


    @Override
    @Transactional
    public List<CRUDorderResponse> getForStaff(String username) {
        List<CRUDorderResponse> orders = new ArrayList<>();
        List<TestResult> testResults = testResultRepository.findAll();
        if(testResults.isEmpty()) {
            throw new IllegalArgumentException("No test results found for the given username: " + username);
        }
        for(TestResult testResult : testResults) {
            if (testResult.getAnalyzedByStaff().getUsername().equalsIgnoreCase(username)) {
                Long orderId = testResult.getOrder().getId();
                ServiceOrder serviceOrder = orderServiceRepository.getById(orderId);
                if (serviceOrder != null) {
                    // Map ServiceOrder to CRUDorderResponse
                   CRUDorderResponse response = orderServiceMapper.toCRUDorderResponse(serviceOrder);
                    if (response != null) {
                        orders.add(response);
                    }
                }
                else{
                    throw new IllegalArgumentException("Service order not found for ID: " + orderId);
                }
            }
        }
        return orders;
    }

    @Override
    @Transactional
    public CRUDorderResponse getOrderById(Long orderId) {
        ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Service order not found for ID: " + orderId));
        CRUDorderResponse response = orderServiceMapper.toCRUDorderResponse(serviceOrder);
        if (response == null) {
            throw new IllegalArgumentException("Failed to map service order to CRUD response");
        }
        return response;
    }

    @Override
    @Transactional
    public List<CRUDsampleCollectionResponse> getSampleCollectionTasks(String username) {
        List<CRUDsampleCollectionResponse> sampleCollectionResponses = new ArrayList<>();
        List<SampleCollection> sampleCollections = sampleCollectionRepository.findAll();
        if (sampleCollections.isEmpty()) {
            throw new IllegalArgumentException("No sample collections found for the given username: " + username);
        }
        for(SampleCollection sampleCollection : sampleCollections) {
            if (sampleCollection.getStaff().getUsername().equalsIgnoreCase(username)) {
                CRUDsampleCollectionResponse response = sampleCollectionMapper.toResponse(sampleCollection);
                if (response != null) {
                    sampleCollectionResponses.add(response);
                }
            }
        }
        return sampleCollectionResponses;
    }

    @Override
    @Transactional
    public CRUDsampleCollectionResponse getSampleCollectionTasksById(Long sampleCollectionId) {
        SampleCollection sampleCollection = sampleCollectionRepository.findById(sampleCollectionId)
                .orElseThrow(() -> new IllegalArgumentException("Sample collection not found for ID: " + sampleCollectionId));
        CRUDsampleCollectionResponse response = sampleCollectionMapper.toResponse(sampleCollection);
        if (response == null) {
            throw new IllegalArgumentException("Failed to map sample collection to response");
        }
        return response;

    }

    @Override
    @Transactional
    public void updateSampleCollectionStatus(Long sampleCollectionId, String collectionStatus, String sampleQuality) {
            SampleCollection sampleCollection = sampleCollectionRepository.findById(sampleCollectionId)
                    .orElseThrow(() -> new IllegalArgumentException("Sample collection not found for ID: " + sampleCollectionId));
            if(sampleCollection.getCollectionStatus().equals(CollectionStatus.CANCELLED)){
                throw new IllegalArgumentException("Cannot update a cancelled sample collection");
            }
            if(sampleCollection.getCollectionStatus().equals(CollectionStatus.COLLECTED)){
                throw new IllegalArgumentException("Cannot update a completed sample collection");
            }
            sampleCollection.setCollectionStatus(CollectionStatus.valueOf(collectionStatus));
            sampleCollection.setSampleQuality(SampleQuality.valueOf(sampleQuality));
            sampleCollectionRepository.save(sampleCollection);
    }

    @Override
    public List<TestResultsResponse> getTestResults(String username) {
        List<TestResultsResponse> testResultsResponses = new ArrayList<>();
        List<TestResult> testResults = testResultRepository.findAll();
        if (testResults.isEmpty()) {
            throw new IllegalArgumentException("No test results found for the given username: " + username);
        }
        for (TestResult testResult : testResults) {
            if (testResult.getAnalyzedByStaff().getUsername().equalsIgnoreCase(username)) {
                TestResultsResponse response = testResultsMapper.toResponse(testResult);
                if (response != null) {
                    testResultsResponses.add(response);
                }
            }
        }
        return testResultsResponses;
    }

    @Override
    public TestResultsResponse getTestResultById(Long testResultId) {
        TestResult testResult = testResultRepository.findById(testResultId)
                .orElseThrow(() -> new IllegalArgumentException("Test result not found for ID: " + testResultId));
        TestResultsResponse response = testResultsMapper.toResponse(testResult);
        if (response == null) {
            throw new IllegalArgumentException("Failed to map test result to response");
        }
        return response;
    }

    @Override
    @Transactional
    public void updateTestResult(TestResultsResquest testResultsResquest, Long testResultId) {
        TestResult testResult = testResultRepository.findById(testResultId)
                .orElseThrow(() -> new IllegalArgumentException("Test result not found for ID: " + testResultId));
        testResultsMapper.updateEntityFromRequest(testResultsResquest, testResult);
        testResultRepository.save(testResult);
    }

    @Override
    public List<RawDataResponse> getRawData(String username) {
        // not implemented yet
        return List.of();
    }

    @Override
    public RawDataResponse getRawDataById(Long rawDataId) {
        RawTestData rawTestData = rawTestDataRepository.findById(rawDataId)
                .orElseThrow(() -> new IllegalArgumentException("Raw data not found for ID: " + rawDataId));
        RawDataResponse response = rawDataMapper.toResponse(rawTestData);
        if (response == null) {
            throw new IllegalArgumentException("Failed to map raw data to response");
        }
        return response;
    }

    @Override
    @Transactional
    public void updateRawData(RawDataRequest rawDataRequest, Long rawDataId) {
        RawTestData rawTestData = rawTestDataRepository.findById(rawDataId)
                .orElseThrow(() -> new IllegalArgumentException("Raw data not found for ID: " + rawDataId));
        rawDataMapper.updateEntityFromRequest(rawDataRequest, rawTestData);
        rawTestDataRepository.save(rawTestData);
    }

    @Override
    @Transactional
    public void createRawData(RawDataRequest rawDataRequest, Long testResultId) {
        RawTestData rawTestData = rawDataMapper.toEntity(rawDataRequest);
        if (rawTestData == null) {
            throw new IllegalArgumentException("Failed to map raw data request to entity");
        }
        TestResult testResult = testResultRepository.findById(testResultId)
                .orElseThrow(() -> new IllegalArgumentException("Test result not found for ID: " + testResultId));
        RawTestData rawTestData1 = rawTestDataRepository.save(rawTestData);
        testResult.setRawData(rawTestData1);
        testResultRepository.save(testResult);
    }

    @Override
    public List<CRUDorderResponse> filterOrdersByStatus(String status, String username) {
        List<CRUDorderResponse> filteredOrders = new ArrayList<>();
        List<TestResult> testResults = testResultRepository.findAll();
        if (testResults.isEmpty()) {
            throw new IllegalArgumentException("No test results found for the given username: " + username);
        }
        for (TestResult testResult : testResults) {
            if (testResult.getAnalyzedByStaff().getUsername().equalsIgnoreCase(username) &&
                testResult.getOrder().getOrderStatus().toString().equalsIgnoreCase(status)) {
                ServiceOrder serviceOrder = orderServiceRepository.getById(testResult.getOrder().getId());
                if (serviceOrder != null) {
                    CRUDorderResponse response = orderServiceMapper.toCRUDorderResponse(serviceOrder);
                    if (response != null) {
                        filteredOrders.add(response);
                    }
                } else {
                    throw new IllegalArgumentException("Service order not found for ID: " + testResult.getOrder().getId());
                }
            }
        }
        return filteredOrders;
    }

    @Override
    public List<CRUDsampleCollectionResponse> filterSampleCollectionsByStatus(String status, String username) {
        List<CRUDsampleCollectionResponse> filteredSampleCollections = new ArrayList<>();
        List<SampleCollection> sampleCollections = sampleCollectionRepository.findAll();
        if (sampleCollections.isEmpty()) {
            throw new IllegalArgumentException("No sample collections found for the given username: " + username);
        }
        for (SampleCollection sampleCollection : sampleCollections) {
            if (sampleCollection.getStaff().getUsername().equalsIgnoreCase(username) &&
                sampleCollection.getCollectionStatus().toString().equalsIgnoreCase(status)) {
                CRUDsampleCollectionResponse response = sampleCollectionMapper.toResponse(sampleCollection);
                if (response != null) {
                    filteredSampleCollections.add(response);
                }
            }
        }
        return filteredSampleCollections;
    }

    @Override
    public List<TestResultsResponse> filterTestResultsByStatus(String status, String username) {
        List<TestResultsResponse> filteredTestResults = new ArrayList<>();
        List<TestResult> testResults = testResultRepository.findAll();
        if (testResults.isEmpty()) {
            throw new IllegalArgumentException("No test results found for the given username: " + username);
        }
        for (TestResult testResult : testResults) {
            if (testResult.getAnalyzedByStaff().getUsername().equalsIgnoreCase(username) &&
                testResult.getResultStatus().toString().equalsIgnoreCase(status)) {
                TestResultsResponse response = testResultsMapper.toResponse(testResult);
                if (response != null) {
                    filteredTestResults.add(response);
                }
            }
        }
        return filteredTestResults;
    }

    @Override
    public CRUDorderResponse searchOrderById(Long orderId, String username) {
        List<TestResult> testResults = testResultRepository.findAll();
        for (TestResult testResult : testResults) {
            if (testResult.getAnalyzedByStaff().getUsername().equalsIgnoreCase(username)) {
                if (testResult.getOrder().getId().equals(orderId)) {
                    ServiceOrder serviceOrder = orderServiceRepository.findById(orderId)
                            .orElseThrow(() -> new IllegalArgumentException("Service order not found for ID: " + orderId));
                    CRUDorderResponse response = orderServiceMapper.toCRUDorderResponse(serviceOrder);
                    return response;
                }
            }
        }
        return null;
    }

    @Override
    public CRUDsampleCollectionResponse searchSampleCollectionById(Long sampleCollectionId, String username) {
        List<SampleCollection> sampleCollections = sampleCollectionRepository.findAll();
        for (SampleCollection sampleCollection : sampleCollections) {
            if (sampleCollection.getStaff().getUsername().equalsIgnoreCase(username)) {
                if (sampleCollection.getCollectionId().equals(sampleCollectionId)) {
                    CRUDsampleCollectionResponse response = sampleCollectionMapper.toResponse(sampleCollection);
                    return response;
                }
            }
        }
        return null;
    }

    @Override
    public TestResultsResponse searchTestResultById(Long testResultId, String username) {
        List<TestResult> testResults = testResultRepository.findAll();
        for (TestResult testResult : testResults) {
            if (testResult.getAnalyzedByStaff().getUsername().equalsIgnoreCase(username)) {
                if (testResult.getId().equals(testResultId)) {
                    TestResultsResponse response = testResultsMapper.toResponse(testResult);
                    return response;
                }
            }
        }
        return null;
    }
}
