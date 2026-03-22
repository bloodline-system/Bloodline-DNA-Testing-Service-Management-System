package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.ServiceFeatureRequest;
import com.dna_testing_system.dev.dto.response.ServiceFeatureResponse;
import com.dna_testing_system.dev.dto.response.ServiceTypeResponse;
import com.dna_testing_system.dev.entity.ServiceFeature;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceFeatureMapper {
    ServiceFeatureResponse toResponse(ServiceFeature serviceFeature);
    ServiceFeature toEntity(ServiceFeatureRequest serviceFeatureRequest);
    List<ServiceFeatureResponse> toResponse(List<ServiceFeature> serviceFeatures);
}
