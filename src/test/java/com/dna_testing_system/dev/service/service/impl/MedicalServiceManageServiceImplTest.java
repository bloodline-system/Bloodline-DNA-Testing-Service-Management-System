package com.dna_testing_system.dev.service.service.impl;

import com.dna_testing_system.dev.dto.response.medical_service.MedicalServiceResponse;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.enums.ServiceCategory;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.MedicalServiceException;
import com.dna_testing_system.dev.mapper.MedicalServiceMapper;
import com.dna_testing_system.dev.mapper.ServiceFeatureMapper;
import com.dna_testing_system.dev.mapper.ServiceTypeMapper;
import com.dna_testing_system.dev.repository.MedicalServiceRepository;
import com.dna_testing_system.dev.repository.ServiceFeatureRepository;
import com.dna_testing_system.dev.repository.ServiceTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalServiceManageServiceImplTest {

    @Mock
    ServiceTypeRepository serviceTypeRepository;
    @Mock
    ServiceTypeMapper serviceTypeMapper;
    @Mock
    ServiceFeatureMapper serviceFeatureMapper;
    @Mock
    ServiceFeatureRepository serviceFeatureRepository;
    @Mock
    MedicalServiceMapper medicalServiceMapper;
    @Mock
    MedicalServiceRepository medicalServiceRepository;

    @InjectMocks
    MedicalServiceManageServiceImpl service;

    @Test
    void getAllServices_mapsRepositoryResultsThroughMapper() {
        List<MedicalService> entities = List.of(
                MedicalService.builder()
                        .id(1L)
                        .serviceName("S1")
                        .serviceCategory(ServiceCategory.CIVIL)
                        .build());
        List<MedicalServiceResponse> responses = List.of(
                MedicalServiceResponse.builder().id(1L).serviceName("S1").build());

        when(medicalServiceRepository.findAll()).thenReturn(entities);
        when(medicalServiceMapper.toResponse(entities)).thenReturn(responses);

        List<MedicalServiceResponse> result = service.getAllServices();

        assertThat(result).isSameAs(responses);
        verify(medicalServiceMapper).toResponse(entities);
    }

    @Test
    void getServiceById_found_returnsMappedResponse() {
        MedicalService entity = MedicalService.builder()
                .id(10L)
                .serviceName("X")
                .serviceCategory(ServiceCategory.CIVIL)
                .build();
        MedicalServiceResponse response = MedicalServiceResponse.builder()
                .id(10L)
                .serviceName("X")
                .build();

        when(medicalServiceRepository.findById(10L)).thenReturn(Optional.of(entity));
        when(medicalServiceMapper.toResponse(entity)).thenReturn(response);

        assertThat(service.getServiceById(10L)).isEqualTo(response);
    }

    @Test
    void getServiceById_missing_throwsMedicalServiceException() {
        when(medicalServiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getServiceById(99L))
                .isInstanceOf(MedicalServiceException.class)
                .satisfies(ex -> assertThat(((MedicalServiceException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.MEDICAL_SERVICE_NOT_EXISTS));
    }
}
