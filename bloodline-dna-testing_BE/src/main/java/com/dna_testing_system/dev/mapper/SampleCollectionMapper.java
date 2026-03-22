package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.SampleCollectionRequest;
import com.dna_testing_system.dev.dto.response.SampleCollectionResponse;
import com.dna_testing_system.dev.entity.SampleCollection;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.User;
import org.springframework.stereotype.Component;

@Component
public class SampleCollectionMapper {

    public SampleCollection toEntity(SampleCollectionRequest request, ServiceOrder order, User staff) {
        return SampleCollection.builder()
                .order(order)
                .staff(staff)
                .collectionDate(request.getCollectionDate())
                .sampleQuality(request.getSampleQuality())
                .collectionStatus(request.getCollectionStatus())
                .sampleType(request.getSampleType())
                .build();
    }

    public SampleCollectionResponse toResponse(SampleCollection entity) {
        SampleCollectionResponse response = new SampleCollectionResponse();
        response.setId(entity.getCollectionId());
        response.setCollectionDateTime(entity.getCollectionDate());
        response.setCollectedBy(entity.getStaff().getFullName());
        response.setSampleId("SAMPLE-" + entity.getCollectionId());
        response.setSampleType(entity.getSampleType().name());
        response.setSampleQuality(entity.getSampleQuality());
        response.setConditionNotes(entity.getCollectionStatus().name());
        return response;
    }
}
