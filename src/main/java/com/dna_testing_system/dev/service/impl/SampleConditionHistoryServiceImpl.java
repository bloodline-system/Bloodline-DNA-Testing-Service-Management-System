package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.SampleConditionHistoryRequest;
import com.dna_testing_system.dev.service.SampleConditionHistoryService;
import org.springframework.stereotype.Service;

@Service
public class SampleConditionHistoryServiceImpl implements SampleConditionHistoryService {

    @Override
    public void recordCondition(Long sampleId, SampleConditionHistoryRequest request) {
        // TODO: Implement logic to save condition history to DB or process request
        System.out.println("Recording condition for sampleId = " + sampleId);
    }
}
