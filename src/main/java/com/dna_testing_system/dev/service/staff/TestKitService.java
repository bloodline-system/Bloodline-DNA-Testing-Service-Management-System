package com.dna_testing_system.dev.service.staff;

import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TestKitService {
    void CreateTestKit(TestKitRequest testKitRequest);
    List<TestKitResponse> GetTestKitResponseByName(String kitName);
    List<TestKitResponse> GetTestKitResponseList();
    Page<TestKitResponse> getTestKitsPage(Pageable pageable);
    void UpdateTestKit(Long kitId, TestKitRequest testKitRequest);
    void DeleteTestKit(Long kitId);
    TestKitResponse GetTestKitResponseById(Long kitId);
    List<TestKitResponse> searchTestKits(String searchQuery);
    Page<TestKitResponse> searchTestKitsPage(String searchQuery, Pageable pageable);
}
