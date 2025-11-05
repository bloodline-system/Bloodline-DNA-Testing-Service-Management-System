package com.dna_testing_system.dev.service;

import com.dna_testing_system.dev.dto.request.MedicalServiceRequest;
import com.dna_testing_system.dev.dto.request.MedicalServiceUpdateRequest;
import com.dna_testing_system.dev.dto.request.ServiceFeatureRequest;
import com.dna_testing_system.dev.dto.request.ServiceTypeRequest;
import com.dna_testing_system.dev.dto.response.MedicalServiceFilterResponse;
import com.dna_testing_system.dev.dto.response.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.ServiceFeatureResponse;
import com.dna_testing_system.dev.dto.response.ServiceTypeResponse;

import java.util.List;

public interface MedicalServiceManageService {

    MedicalServiceResponse createService(MedicalServiceRequest request);

    List<MedicalServiceResponse> getAllServices();

    MedicalServiceResponse getServiceById(Long id);
    List<MedicalServiceFilterResponse> searchServiceByNameContaining(String name);

    void deleteService(Long id);
    void updateService(Long id, MedicalServiceUpdateRequest request);

    List<ServiceTypeResponse> getAllServiceTypes();

    ServiceTypeResponse createServiceType(ServiceTypeRequest request);
    List<ServiceFeatureResponse> getAllServiceFeatures();
    void deleteTypeService(Long id);
    void updateTypeService(Long id, ServiceTypeRequest request);

    ServiceFeatureResponse createServiceFeature(ServiceFeatureRequest request);
    void deleteServiceFeature(Long id);
}
