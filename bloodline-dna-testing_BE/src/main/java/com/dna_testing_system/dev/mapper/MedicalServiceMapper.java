package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.MedicalServiceRequest;
import com.dna_testing_system.dev.dto.request.MedicalServiceUpdateRequest;
import com.dna_testing_system.dev.dto.response.MedicalServiceFilterResponse;
import com.dna_testing_system.dev.dto.response.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.ServiceFeatureAssignmentResponse;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.ServiceFeature;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {ServiceTypeMapper.class})
public interface MedicalServiceMapper {

    @Mapping(target = "serviceTypeId", source = "serviceType.id")
    @Mapping(target = "serviceTypeName", source = "serviceType.typeName")
    @Mapping(target = "features", source = "serviceFeatures")
    MedicalServiceResponse toResponse(MedicalService service);

    List<MedicalServiceResponse> toResponse(List<MedicalService> services);
    List<MedicalServiceFilterResponse> toResultFilter(List<MedicalService> services);
    @Mapping(target = "serviceFeatures", source = "editFeatureAssignments")
    void updateMedicalService(MedicalServiceUpdateRequest updateRequest, @MappingTarget MedicalService medicalService);

    @Mapping(target = "featureId", source = "id")
    @Mapping(target = "featureName", source = "featureName")
    @Mapping(target = "isAvailable", source = "isAvailable")
    ServiceFeatureAssignmentResponse toServiceFeatureAssignmentResponse(ServiceFeature feature);

    List<ServiceFeatureAssignmentResponse> toServiceFeatureAssignmentResponseList(Set<ServiceFeature> features);
    // Convert from Request to Entity, ignoring the serviceType and features that need special handling
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "serviceType", ignore = true) 
    @Mapping(target = "serviceFeatures", ignore = true)
    @Mapping(target = "serviceOrders", ignore = true)
    @Mapping(target = "customerFeedbacks", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MedicalService toEntity(MedicalServiceRequest request);
}
