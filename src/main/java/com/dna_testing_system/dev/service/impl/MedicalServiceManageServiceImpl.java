package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.request.*;
import com.dna_testing_system.dev.dto.response.MedicalServiceFilterResponse;
import com.dna_testing_system.dev.dto.response.MedicalServiceResponse;
import com.dna_testing_system.dev.dto.response.ServiceFeatureResponse;
import com.dna_testing_system.dev.dto.response.ServiceTypeResponse;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.ServiceFeature;
import com.dna_testing_system.dev.entity.ServiceType;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.MedicalServiceException;
import com.dna_testing_system.dev.mapper.MedicalServiceMapper;
import com.dna_testing_system.dev.mapper.ServiceFeatureMapper;
import com.dna_testing_system.dev.mapper.ServiceTypeMapper;
import com.dna_testing_system.dev.repository.MedicalServiceRepository;
import com.dna_testing_system.dev.repository.ServiceFeatureRepository;
import com.dna_testing_system.dev.repository.ServiceTypeRepository;
import com.dna_testing_system.dev.service.MedicalServiceManageService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,  makeFinal = true)
public class MedicalServiceManageServiceImpl implements MedicalServiceManageService {

    ServiceTypeRepository serviceTypeRepository;
    ServiceTypeMapper serviceTypeMapper;
    ServiceFeatureMapper  serviceFeatureMapper;
    ServiceFeatureRepository serviceFeatureRepository;
    MedicalServiceMapper medicalServiceMapper;
    MedicalServiceRepository medicalServiceRepository;

    @Override
    @Transactional
    public MedicalServiceResponse createService(MedicalServiceRequest request) {
        // Find service type
        ServiceType serviceType = serviceTypeRepository.findById(request.getServiceTypeId())
                .orElseThrow(() -> new MedicalServiceException(ErrorCode.SERVICE_TYPE_NOT_EXISTS));

        // Create new service using the mapper
        MedicalService service = medicalServiceMapper.toEntity(request);
        service.setServiceType(serviceType);

        // Save service
        MedicalService savedService = medicalServiceRepository.save(service);
        if (savedService.getServiceFeatures() == null)
            savedService.setServiceFeatures(new HashSet<>());
        // Create service features
        if (request.getFeatureAssignments() != null && !request.getFeatureAssignments().isEmpty()) {
            for (FeatureAssignmentCreationRequest featureRequest : request.getFeatureAssignments()) {
                ServiceFeature feature = ServiceFeature.builder()
                        .service(savedService)
                        .featureName(featureRequest.getFeatureName())
                        .isAvailable(featureRequest.getIsAvailable())
                        .build();
                serviceFeatureRepository.save(feature);
                savedService.getServiceFeatures().add(feature);
            }
        }

        // Refresh service to get updated features
        MedicalService refreshedService = medicalServiceRepository.findById(savedService.getId())
                .orElseThrow(() -> new MedicalServiceException(ErrorCode.MEDICAL_SERVICE_NOT_EXISTS));
//        refreshedService.getServiceFeatures().addAll(savedService.getServiceFeatures());
        return medicalServiceMapper.toResponse(refreshedService);
    }

    @Override
    public List<MedicalServiceResponse> getAllServices() {
        List<MedicalService> services = medicalServiceRepository.findAll();
        return medicalServiceMapper.toResponse(services);
    }

    @Override
    public MedicalServiceResponse getServiceById(Long id) {
        MedicalService service = medicalServiceRepository.findById(id)
                .orElseThrow(() -> new MedicalServiceException(ErrorCode.MEDICAL_SERVICE_NOT_EXISTS));
        return medicalServiceMapper.toResponse(service);
    }

    @Override
    public List<MedicalServiceFilterResponse> searchServiceByNameContaining(String name) {
        return medicalServiceMapper.toResultFilter(medicalServiceRepository.searchAllByServiceNameContaining(name));
    }

    @Override
    @Transactional
    public void deleteService(Long id) {
        MedicalService service = medicalServiceRepository.findById(id)
                .orElseThrow(() -> new MedicalServiceException(ErrorCode.MEDICAL_SERVICE_NOT_EXISTS));

        // Delete service (with cascade delete for features)
        medicalServiceRepository.delete(service);
    }

    @Override
    @Transactional
    public void updateService(Long id, MedicalServiceUpdateRequest request) {
        var medicalService = medicalServiceRepository.findById(id)
                .orElseThrow(() ->  new MedicalServiceException(ErrorCode.MEDICAL_SERVICE_NOT_EXISTS));
        //delete service feature
        for (var feature : medicalService.getServiceFeatures()) {
            deleteServiceFeature(feature.getId());
        }
        try {
            medicalServiceMapper.updateMedicalService(request, medicalService);
            if (medicalService.getServiceFeatures() == null)
                medicalService.setServiceFeatures(new HashSet<>());
            else
                medicalService.getServiceFeatures().clear();
            //assign for new medical service
            if (request.getEditFeatureAssignments() != null && !request.getEditFeatureAssignments().isEmpty()) {
                for (FeatureAssignmentCreationRequest featureRequest : request.getEditFeatureAssignments()) {
                    ServiceFeature feature = ServiceFeature.builder()
                            .service(medicalService)
                            .featureName(featureRequest.getFeatureName())
                            .isAvailable(featureRequest.getIsAvailable())
                            .build();
                    serviceFeatureRepository.save(feature);
                    medicalService.getServiceFeatures().add(feature);
                }
            }
            medicalServiceRepository.save(medicalService);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MedicalServiceException(ErrorCode.UNKNOWN_ERROR);
        }
    }

    @Override
    public List<ServiceTypeResponse> getAllServiceTypes() {
        List<ServiceType> serviceTypeList = serviceTypeRepository.findAll();
        return serviceTypeMapper.toResponse(serviceTypeList);
    }

    @Override
    @Transactional
    public ServiceTypeResponse createServiceType(ServiceTypeRequest request) {
        request.setTypeName(request.getTypeName().toUpperCase());
        if (serviceTypeRepository.existsByTypeName(request.getTypeName()))
            throw new MedicalServiceException(ErrorCode.SERVICE_TYPE_EXISTS);
        ServiceType serviceType = ServiceType.builder()
                .typeName(request.getTypeName())
                .isActive(request.getIsActive())
                .build();
        serviceTypeRepository.save(serviceType);
        return serviceTypeMapper.toResponse(serviceType);
    }
    @Override
    @Transactional
    public List<ServiceFeatureResponse> getAllServiceFeatures() {
        List<ServiceFeature> serviceFeatureList = serviceFeatureRepository.findAll();
        return serviceFeatureMapper.toResponse(serviceFeatureList);
    }

    @Override
    public void deleteTypeService(Long id) {
        var serviceType = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new MedicalServiceException(ErrorCode.SERVICE_TYPE_NOT_EXISTS));

        serviceTypeRepository.delete(serviceType);
    }

    @Override
    public void updateTypeService(Long id, ServiceTypeRequest request) {
        var serviceType = serviceTypeRepository.findById(id)
                .orElseThrow(() -> new MedicalServiceException(ErrorCode.SERVICE_TYPE_NOT_EXISTS));
        request.setTypeName(request.getTypeName().toUpperCase());

        serviceTypeMapper.updateServiceType(request,serviceType);
        serviceTypeRepository.save(serviceType);
    }

    @Override
    @Transactional
    public ServiceFeatureResponse createServiceFeature(ServiceFeatureRequest request) {
        ServiceFeature serviceFeature = ServiceFeature.builder()
                .featureName(request.getFeatureName())
                .isAvailable(request.getIsAvailable())
                .build();
        serviceFeatureRepository.save(serviceFeature);
        return serviceFeatureMapper.toResponse(serviceFeature);
    }

    @Override
    public void deleteServiceFeature(Long id) {
        var serviceFeature = serviceFeatureRepository.findById(id)
                .orElseThrow(() -> new MedicalServiceException(ErrorCode.SERVICE_FEATURE_NOT_EXISTS));
        serviceFeatureRepository.delete(serviceFeature);
    }
}
