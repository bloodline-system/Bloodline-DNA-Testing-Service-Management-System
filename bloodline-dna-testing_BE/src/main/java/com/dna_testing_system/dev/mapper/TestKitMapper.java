package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.TestKitRequest;
import com.dna_testing_system.dev.dto.response.TestKitResponse;
import com.dna_testing_system.dev.entity.TestKit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TestKitMapper {
    // Define mapping methods here if needed
    // For example, you can map between DTOs and entities related to TestKit
    // Example:
    @Mapping(target = "id", source = "testKit.id")
    @Mapping(target = "quantityInStock", source = "testKit.quantityInStock")
    TestKitResponse toResponse(TestKit testKit);
    TestKit toEntity(TestKitRequest request);
    void updateEntityFromDto(TestKitRequest request,@MappingTarget TestKit testKit);
}
