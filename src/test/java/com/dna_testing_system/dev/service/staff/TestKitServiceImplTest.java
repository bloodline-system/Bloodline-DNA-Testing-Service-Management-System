package com.dna_testing_system.dev.service.staff;

import com.dna_testing_system.dev.dto.request.test_kit.TestKitRequest;
import com.dna_testing_system.dev.dto.response.test_kit.TestKitResponse;
import com.dna_testing_system.dev.entity.OrderKit;
import com.dna_testing_system.dev.entity.TestKit;
import com.dna_testing_system.dev.mapper.TestKitMapper;
import com.dna_testing_system.dev.repository.OrderTestKitRepository;
import com.dna_testing_system.dev.repository.TestKitRepository;
import com.dna_testing_system.dev.service.staff.impl.TestKitServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.asserts.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestKitServiceImplTest {

    @Mock
    private TestKitRepository testKitRepository;

    @Mock
    private OrderTestKitRepository orderTestKitRepository;

    @Mock
    private TestKitMapper testKitMapper;

    @InjectMocks
    private TestKitServiceImpl testKitService;

    private TestKitRequest testKitRequest;
    private TestKit testKit;

    @BeforeEach
    void setUp() {
        testKitRequest = TestKitRequest.builder()
                .kitName("S1")
                .kitType(com.dna_testing_system.dev.enums.KitType.PATERNITY)
                .sampleType(com.dna_testing_system.dev.enums.SampleType.BLOOD)
                .basePrice(BigDecimal.valueOf(100))
                .currentPrice(BigDecimal.valueOf(90))
                .quantityInStock(5)
                .kitDescription("desc")
                .expiryDate(LocalDate.now().plusDays(30))
                .producedBy("abc")
                .isAvailable(true)
                .build();

        testKit = TestKit.builder()
                .id(1L)
                .kitName("S1")
                .kitType("GENETIC")
                .sampleType("BLOOD")
                .basePrice(BigDecimal.valueOf(100))
                .currentPrice(BigDecimal.valueOf(90))
                .quantityInStock(5)
                .kitDescription("desc")
                .expiryDate(LocalDate.now().plusDays(30))
                .producedBy("abc")
                .isAvailable(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createTestKit_callsRepositorySave() {
        when(testKitMapper.toEntity(testKitRequest)).thenReturn(testKit);

        testKitService.CreateTestKit(testKitRequest);

        verify(testKitRepository).save(testKit);
    }

    @Test
    void getTestKitResponseList_returnsMapped() {
        TestKitResponse response = TestKitResponse.builder().id(1L).kitName("S1").build();
        when(testKitRepository.findAll()).thenReturn(List.of(testKit));
        when(testKitMapper.toResponse(testKit)).thenReturn(response);

        List<TestKitResponse> result = testKitService.GetTestKitResponseList();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKitName()).isEqualTo("S1");
        verify(testKitMapper).toResponse(testKit);
    }

    @Test
    void getTestKitResponseById_whenFound_returnsResponse() {
        TestKitResponse response = TestKitResponse.builder().id(1L).kitName("S1").build();
        when(testKitRepository.findById(1L)).thenReturn(Optional.of(testKit));
        when(testKitMapper.toResponse(testKit)).thenReturn(response);

        TestKitResponse result = testKitService.GetTestKitResponseById(1L);

        assertThat(result.getKitName()).isEqualTo("S1");
        verify(testKitMapper).toResponse(testKit);
    }

    @Test
    void getTestKitResponseById_whenNotFound_throws() {
        when(testKitRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> testKitService.GetTestKitResponseById(999L));
    }

    @Test
    void updateTestKit_whenFound_updatesAndSaves() {
        when(testKitRepository.findById(1L)).thenReturn(Optional.of(testKit));

        testKitService.UpdateTestKit(1L, testKitRequest);

        verify(testKitMapper).updateEntityFromDto(testKitRequest, testKit);
        verify(testKitRepository).save(testKit);
    }

    @Test
    void updateTestKit_whenNotFound_throws() {
        when(testKitRepository.findById(123L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> testKitService.UpdateTestKit(123L, testKitRequest));
    }

    @Test
    void deleteTestKit_whenOrderKitExists_setsUnavailable() {
        OrderKit orderKit = OrderKit.builder().id(15L).kit(testKit).build();
        when(orderTestKitRepository.findAll()).thenReturn(List.of(orderKit));
        when(testKitRepository.findById(1L)).thenReturn(Optional.of(testKit));

        testKitService.DeleteTestKit(1L);

        assertThat(testKit.getIsAvailable()).isFalse();
        verify(testKitRepository).save(testKit);
        verify(testKitRepository, never()).deleteById(1L);
    }

    @Test
    void deleteTestKit_whenNoOrderKit_deletesById() {
        when(orderTestKitRepository.findAll()).thenReturn(List.of());
        when(testKitRepository.findById(1L)).thenReturn(Optional.of(testKit));

        testKitService.DeleteTestKit(1L);

        verify(testKitRepository).deleteById(1L);
    }

    @Test
    void searchTestKits_filtersByName() {
        TestKit another = TestKit.builder().id(2L).kitName("SearchKit").kitType("GENETIC").sampleType("BLOOD").build();
        TestKitResponse mapped = TestKitResponse.builder().id(2L).kitName("SearchKit").build();

        when(testKitRepository.findAll()).thenReturn(List.of(testKit, another));
        when(testKitMapper.toResponse(another)).thenReturn(mapped);

        List<TestKitResponse> result = testKitService.searchTestKits("search");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKitName()).isEqualTo("SearchKit");
    }

    @Test
    void getTestKitsPage_returnsPage() {
        TestKit entity1 = TestKit.builder().id(1L).kitName("p1").build();
        TestKit entity2 = TestKit.builder().id(2L).kitName("p2").build();

        Page<TestKit> page = new PageImpl<>(List.of(entity1, entity2), PageRequest.of(0, 10), 2);
        when(testKitRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(testKitMapper.toResponse(entity1)).thenReturn(TestKitResponse.builder().id(1L).kitName("p1").build());
        when(testKitMapper.toResponse(entity2)).thenReturn(TestKitResponse.builder().id(2L).kitName("p2").build());

        Page<TestKitResponse> result = testKitService.getTestKitsPage(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void searchTestKitsPage_returnsPagedResults() {
        TestKit entity1 = TestKit.builder().id(1L).kitName("SearchKit").build();
        TestKit entity2 = TestKit.builder().id(2L).kitName("Other").build();
        TestKitResponse response1 = TestKitResponse.builder().id(1L).kitName("SearchKit").build();

        when(testKitRepository.findAll()).thenReturn(List.of(entity1, entity2));
        when(testKitMapper.toResponse(entity1)).thenReturn(response1);

        Page<TestKitResponse> page = testKitService.searchTestKitsPage("search", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getKitName()).isEqualTo("SearchKit");
    }
}