package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.SampleCollectionRequest;
import com.dna_testing_system.dev.dto.response.SampleCollectionResponse;
import com.dna_testing_system.dev.entity.SampleCollection;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.mapper.SampleCollectionMapper;
import com.dna_testing_system.dev.repository.SampleCollectionRepository;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.SampleCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SampleCollectionServiceImpl implements SampleCollectionService {

    private final SampleCollectionRepository sampleCollectionRepository;
    private final SampleCollectionMapper sampleCollectionMapper;
    private final OrderServiceRepository orderServiceRepository;
    private final UserRepository userRepository;

    @Override
    public SampleCollectionResponse create(SampleCollectionRequest request) {
        ServiceOrder order = orderServiceRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));
        User staff = userRepository.findById(String.valueOf(request.getStaffId()))
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        SampleCollection sample = sampleCollectionMapper.toEntity(request, order, staff);
        SampleCollection saved = sampleCollectionRepository.save(sample);
        return sampleCollectionMapper.toResponse(saved);
    }

    @Override
    public List<SampleCollectionResponse> getAll() {
        return sampleCollectionRepository.findAll().stream()
                .map(sampleCollectionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SampleCollectionResponse getById(Long id) {
        return sampleCollectionRepository.findById(id)
                .map(sampleCollectionMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Sample not found"));
    }

    @Override
    public void delete(Long id) {
        if (!sampleCollectionRepository.existsById(id)) {
            throw new RuntimeException("Sample not found");
        }
        sampleCollectionRepository.deleteById(id);
    }
}
