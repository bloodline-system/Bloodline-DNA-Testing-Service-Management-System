package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.response.CRUDsampleCollectionResponse;
import com.dna_testing_system.dev.entity.SampleCollection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CRUDsampleCollectionMapper {
    CRUDsampleCollectionResponse toResponse(SampleCollection sampleCollection);
}
