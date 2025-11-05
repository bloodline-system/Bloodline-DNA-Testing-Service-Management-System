package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.SampleCollectionRequest;
import com.dna_testing_system.dev.dto.response.SampleCollectionResponse;

import java.util.List;

public interface SampleCollectionService {
    SampleCollectionResponse create(SampleCollectionRequest request);
    List<SampleCollectionResponse> getAll();
    SampleCollectionResponse getById(Long id);
    void delete(Long id);
}
