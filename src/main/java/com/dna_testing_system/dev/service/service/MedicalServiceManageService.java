package com.dna_testing_system.dev.service.service;

import com.dna_testing_system.dev.dto.request.medical_service.MedicalServiceRequest;
import com.dna_testing_system.dev.dto.request.medical_service.MedicalServiceUpdateRequest;
import com.dna_testing_system.dev.dto.request.ServiceFeatureRequest;
import com.dna_testing_system.dev.dto.request.medical_service.ServiceTypeRequest;
import com.dna_testing_system.dev.dto.response.medical_service.MedicalServiceFilterResponse;
import com.dna_testing_system.dev.dto.response.medical_service.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.ServiceFeatureResponse;
import com.dna_testing_system.dev.dto.response.medical_service.ServiceTypeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MedicalServiceManageService {

    // --- Medical Services ---
    MedicalServiceResponse createService(MedicalServiceRequest request);
    List<MedicalServiceResponse> getAllServices();
    Page<MedicalServiceResponse> getServicesPage(Pageable pageable);
    MedicalServiceResponse getServiceById(Long id);
    List<MedicalServiceFilterResponse> searchServiceByNameContaining(String name);
    Page<MedicalServiceFilterResponse> searchServicesPage(String name, Pageable pageable);
    void updateService(Long id, MedicalServiceUpdateRequest request);
    void deleteService(Long id);

    // --- Service Types ---
    ServiceTypeResponse createServiceType(ServiceTypeRequest request);
    List<ServiceTypeResponse> getAllServiceTypes();
    Page<ServiceTypeResponse> getServiceTypesPage(Pageable pageable);
    ServiceTypeResponse getServiceTypeById(Long id);
    void updateTypeService(Long id, ServiceTypeRequest request);
    void deleteTypeService(Long id);

    // --- Service Features ---
    ServiceFeatureResponse createServiceFeature(ServiceFeatureRequest request);
    List<ServiceFeatureResponse> getAllServiceFeatures();
    void deleteServiceFeature(Long id);
}
