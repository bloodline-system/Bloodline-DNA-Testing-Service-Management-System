package com.dna_testing_system.dev.controller;

import com.dna_testing_system.dev.dto.request.SampleCollectionRequest;
import com.dna_testing_system.dev.dto.request.SampleConditionHistoryRequest;
import com.dna_testing_system.dev.dto.response.SampleCollectionResponse;
import com.dna_testing_system.dev.service.SampleCollectionService;
import com.dna_testing_system.dev.service.SampleConditionHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
public class SampleCollectionController {

    private final SampleCollectionService sampleCollectionService;
    private final SampleConditionHistoryService sampleConditionHistoryService;

    @PostMapping
    public ResponseEntity<SampleCollectionResponse> create(@RequestBody SampleCollectionRequest request) {
        return ResponseEntity.ok(sampleCollectionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SampleCollectionResponse>> getAll() {
        return ResponseEntity.ok(sampleCollectionService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SampleCollectionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sampleCollectionService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sampleCollectionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sampleId}/condition-history")
    public ResponseEntity<Void> addConditionHistory(@PathVariable Long sampleId, @RequestBody SampleConditionHistoryRequest request) {
        sampleConditionHistoryService.recordCondition(sampleId, request);
        return ResponseEntity.ok().build();
    }
}
