package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.TestKitRequest;
import com.dna_testing_system.dev.dto.response.TestKitResponse;

import java.util.List;

public interface TestKitService {
    void CreateTestKit(TestKitRequest testKitRequest);
    List<TestKitResponse> GetTestKitResponseByName(String kitName);
    List<TestKitResponse> GetTestKitResponseList();
    void UpdateTestKit(Long kitId, TestKitRequest testKitRequest);
    void DeleteTestKit(Long kitId);
    TestKitResponse GetTestKitResponseById(Long kitId);

    //fix;
    List<TestKitResponse> searchTestKits(String searchQuery);
}
