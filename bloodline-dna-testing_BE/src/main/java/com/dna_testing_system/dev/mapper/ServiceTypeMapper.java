package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.RegisterRequest;
import com.dna_testing_system.dev.dto.request.ServiceTypeRequest;
import com.dna_testing_system.dev.dto.response.ServiceTypeResponse;
import com.dna_testing_system.dev.entity.ServiceType;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceTypeMapper {
    ServiceType toEntity(ServiceTypeRequest request);
    ServiceTypeResponse toResponse(ServiceType serviceType);
    List<ServiceTypeResponse> toResponse(List<ServiceType> serviceTypeList);
    void updateServiceType(ServiceTypeRequest serviceTypeRequest, @MappingTarget ServiceType serviceType);
}
