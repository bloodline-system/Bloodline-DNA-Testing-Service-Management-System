package com.dna_testing_system.dev.service.impl;

import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.SampleCollection;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.enums.CollectionStatus;
import com.dna_testing_system.dev.enums.CollectionType;
import com.dna_testing_system.dev.enums.ServiceCategory;
import com.dna_testing_system.dev.enums.ServiceOrderStatus;
import com.dna_testing_system.dev.enums.SampleQuality;
import com.dna_testing_system.dev.enums.SampleType;
import com.dna_testing_system.dev.mapper.ServiceOrderMapper;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.SampleCollectionRepository;
import com.dna_testing_system.dev.repository.TestResultRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.EmailSender;
import com.dna_testing_system.dev.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTaskManagementServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    SampleCollectionRepository sampleCollectionRepository;
    @Mock
    TestResultRepository testResultRepository;
    @Mock
    OrderServiceRepository orderServiceRepository;
    @Mock
    EmailSender emailSender;
    @Mock
    ServiceOrderMapper serviceOrderMapper;
    @Mock
    NotificationService notificationService;

    @InjectMocks
    OrderTaskManagementServiceImpl service;

    private static User minimalCustomer() {
        return User.builder()
                .id("cust-1")
                .username("customer1")
                .passwordHash("hash")
                .build();
    }

    private static MedicalService minimalService() {
        return MedicalService.builder()
                .id(1L)
                .serviceName("DNA Test")
                .serviceCategory(ServiceCategory.CIVIL)
                .build();
    }

    private static ServiceOrder baseOrder(Long id, ServiceOrderStatus status, Set<SampleCollection> sampleCollections) {
        return ServiceOrder.builder()
                .id(id)
                .customer(minimalCustomer())
                .service(minimalService())
                .collectionType(CollectionType.LAB_VISIT)
                .orderStatus(status)
                .sampleCollections(sampleCollections != null ? sampleCollections : new HashSet<>())
                .build();
    }

    @Test
    void getServiceOrders_mapsEachOrderThroughMapper() {
        ServiceOrder o1 = baseOrder(1L, ServiceOrderStatus.PENDING, new HashSet<>());
        ServiceOrder o2 = baseOrder(2L, ServiceOrderStatus.COMPLETED, new HashSet<>());
        when(orderServiceRepository.findAll()).thenReturn(List.of(o1, o2));
        when(serviceOrderMapper.toDto(o1)).thenReturn(ServiceOrderResponse.builder().id(1L).build());
        when(serviceOrderMapper.toDto(o2)).thenReturn(ServiceOrderResponse.builder().id(2L).build());

        List<ServiceOrderResponse> result = service.getServiceOrders();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ServiceOrderResponse::getId).containsExactly(1L, 2L);
        verify(serviceOrderMapper).toDto(o1);
        verify(serviceOrderMapper).toDto(o2);
    }

    @Test
    void getNewOrders_returnsOnlyPendingWithNoSampleCollections() {
        ServiceOrder newPending = baseOrder(10L, ServiceOrderStatus.PENDING, new HashSet<>());

        Set<SampleCollection> withCollection = new HashSet<>();
        ServiceOrder pendingButAssigned = baseOrder(11L, ServiceOrderStatus.PENDING, withCollection);
        SampleCollection sc = SampleCollection.builder()
                .collectionId(1L)
                .order(pendingButAssigned)
                .staff(User.builder().id("staff-1").username("staff1").passwordHash("h").build())
                .collectionDate(LocalDateTime.now())
                .sampleQuality(SampleQuality.EXCELLENT)
                .collectionStatus(CollectionStatus.PENDING)
                .sampleType(SampleType.BLOOD)
                .build();
        withCollection.add(sc);

        ServiceOrder completed = baseOrder(12L, ServiceOrderStatus.COMPLETED, new HashSet<>());

        when(orderServiceRepository.findAll()).thenReturn(List.of(newPending, pendingButAssigned, completed));
        when(serviceOrderMapper.toDto(any(ServiceOrder.class))).thenAnswer(inv -> {
            ServiceOrder o = inv.getArgument(0);
            return ServiceOrderResponse.builder().id(o.getId()).build();
        });

        List<ServiceOrderResponse> result = service.getNewOrders();

        assertThat(result).hasSize(1);
        assertThat(result).extracting(ServiceOrderResponse::getId).containsExactly(10L);
        verify(serviceOrderMapper).toDto(newPending);
    }
}
