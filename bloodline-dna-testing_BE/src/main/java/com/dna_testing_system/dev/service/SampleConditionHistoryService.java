package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.SampleConditionHistoryRequest;

public interface SampleConditionHistoryService {
    void recordCondition(Long sampleId, SampleConditionHistoryRequest request);
}
