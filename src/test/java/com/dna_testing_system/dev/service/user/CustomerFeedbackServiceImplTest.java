package com.dna_testing_system.dev.service.user;

import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.request.RespondFeedbackRequest;
import com.dna_testing_system.dev.dto.request.UpdatingFeedbackRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.entity.CustomerFeedback;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.Role;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserRole;
import com.dna_testing_system.dev.enums.RoleType;
import com.dna_testing_system.dev.exception.EntityNotFoundException;
import com.dna_testing_system.dev.exception.ErrorCode;
import com.dna_testing_system.dev.exception.FeedbackException;
import com.dna_testing_system.dev.mapper.CustomerFeedbackMapper;
import com.dna_testing_system.dev.repository.CustomerFeedbackRepository;
import com.dna_testing_system.dev.repository.MedicalServiceRepository;
import com.dna_testing_system.dev.repository.OrderServiceRepository;
import com.dna_testing_system.dev.repository.UserRepository;
import com.dna_testing_system.dev.service.impl.CustomerFeedbackServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerFeedbackServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock MedicalServiceRepository medicalServiceRepository;
    @Mock OrderServiceRepository orderServiceRepository;
    @Mock CustomerFeedbackRepository customerFeedbackRepository;
    @Mock CustomerFeedbackMapper customerFeedbackMapper;

    @InjectMocks CustomerFeedbackServiceImpl service;

    @Test
    void createFeedback_orderIdNull_calculatesAverageAndSaves() {
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .customerId("u1")
                .serviceId(10L)
                .orderId(null)
                .serviceQualityRating(5)
                .staffBehaviorRating(4)
                .timelinessRating(3)
                .feedbackTitle("t")
                .feedbackContent("c")
                .responseRequired(true)
                .build();

        User user = User.builder().id("u1").username("alice").passwordHash("x").build();
        MedicalService svc = MedicalService.builder().id(10L).serviceName("DNA").executionTimeDays(1).build();
        CustomerFeedback entity = CustomerFeedback.builder().id(1L).customer(user).service(svc).build();
        CustomerFeedbackResponse response = CustomerFeedbackResponse.builder().id(1L).overallRating(4.0f).build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(medicalServiceRepository.findById(10L)).thenReturn(Optional.of(svc));
        when(customerFeedbackMapper.toEntity(eq(request), eq(user), eq(svc), isNull())).thenReturn(entity);
        when(customerFeedbackMapper.toResponse(entity)).thenReturn(response);

        CustomerFeedbackResponse result = service.createFeedback(request);

        assertThat(result).isSameAs(response);
        ArgumentCaptor<CustomerFeedback> captor = ArgumentCaptor.forClass(CustomerFeedback.class);
        verify(customerFeedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getOverallRating()).isEqualTo((5 + 4 + 3) / 3.0f);
        verify(orderServiceRepository, never()).findById(anyLong());
    }

    @Test
    void createFeedback_orderIdProvided_loadsOrderAndSaves() {
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .customerId("u1")
                .serviceId(10L)
                .orderId(99L)
                .serviceQualityRating(1)
                .staffBehaviorRating(1)
                .timelinessRating(1)
                .build();

        User user = User.builder().id("u1").username("alice").passwordHash("x").build();
        MedicalService svc = MedicalService.builder().id(10L).serviceName("DNA").executionTimeDays(1).build();
        ServiceOrder order = ServiceOrder.builder().id(99L).customer(user).service(svc).build();
        CustomerFeedback entity = CustomerFeedback.builder().id(1L).customer(user).service(svc).order(order).build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(medicalServiceRepository.findById(10L)).thenReturn(Optional.of(svc));
        when(orderServiceRepository.findById(99L)).thenReturn(Optional.of(order));
        when(customerFeedbackMapper.toEntity(eq(request), eq(user), eq(svc), eq(order))).thenReturn(entity);
        when(customerFeedbackMapper.toResponse(entity)).thenReturn(CustomerFeedbackResponse.builder().id(1L).build());

        service.createFeedback(request);

        verify(orderServiceRepository).findById(99L);
        verify(customerFeedbackRepository).save(any(CustomerFeedback.class));
    }

    @Test
    void createFeedback_userNotFound_throwsEntityNotFound() {
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .customerId("missing")
                .serviceId(10L)
                .serviceQualityRating(5)
                .staffBehaviorRating(5)
                .timelinessRating(5)
                .build();
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFeedback(request))
                .isInstanceOf(EntityNotFoundException.class);

        verifyNoInteractions(medicalServiceRepository, orderServiceRepository, customerFeedbackRepository, customerFeedbackMapper);
    }

    @Test
    void createFeedback_serviceNotFound_throwsEntityNotFound() {
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .customerId("u1")
                .serviceId(10L)
                .serviceQualityRating(5)
                .staffBehaviorRating(5)
                .timelinessRating(5)
                .build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(User.builder().id("u1").username("a").passwordHash("x").build()));
        when(medicalServiceRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFeedback(request))
                .isInstanceOf(EntityNotFoundException.class);

        verify(customerFeedbackRepository, never()).save(any());
    }

    @Test
    void createFeedback_mapperThrows_wrapsToFeedbackException() {
        CreateFeedbackRequest request = CreateFeedbackRequest.builder()
                .customerId("u1")
                .serviceId(10L)
                .serviceQualityRating(5)
                .staffBehaviorRating(5)
                .timelinessRating(5)
                .build();
        User user = User.builder().id("u1").username("alice").passwordHash("x").build();
        MedicalService svc = MedicalService.builder().id(10L).serviceName("DNA").executionTimeDays(1).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(medicalServiceRepository.findById(10L)).thenReturn(Optional.of(svc));
        when(customerFeedbackMapper.toEntity(eq(request), eq(user), eq(svc), isNull()))
                .thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> service.createFeedback(request))
                .isInstanceOf(FeedbackException.class)
                .satisfies(ex -> assertThat(((FeedbackException) ex).getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_PERSIST_ERROR));

        verify(customerFeedbackRepository, never()).save(any());
    }

    @Test
    void getFeedbackByCustomer_repositoryThrows_wrapsToFeedbackException() {
        when(customerFeedbackRepository.findByCustomer_Id("u1")).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> service.getFeedbackByCustomer("u1"))
                .isInstanceOf(FeedbackException.class)
                .satisfies(ex -> assertThat(((FeedbackException) ex).getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_GETTING_ERROR));
    }

    @Test
    void getFeedbackByCustomer_mapsList() {
        CustomerFeedback f1 = CustomerFeedback.builder().id(1L).build();
        CustomerFeedback f2 = CustomerFeedback.builder().id(2L).build();
        when(customerFeedbackRepository.findByCustomer_Id("u1")).thenReturn(List.of(f1, f2));
        when(customerFeedbackMapper.toResponse(f1)).thenReturn(CustomerFeedbackResponse.builder().id(1L).build());
        when(customerFeedbackMapper.toResponse(f2)).thenReturn(CustomerFeedbackResponse.builder().id(2L).build());

        List<CustomerFeedbackResponse> result = service.getFeedbackByCustomer("u1");

        assertThat(result).extracting(CustomerFeedbackResponse::getId).containsExactly(1L, 2L);
        verify(customerFeedbackMapper).toResponse(f1);
        verify(customerFeedbackMapper).toResponse(f2);
    }

    @Test
    void getAllFeedbacks_delegatesToRepositoryAndMaps() {
        CustomerFeedback entity = CustomerFeedback.builder().id(1L).feedbackTitle("abc").build();
        when(customerFeedbackRepository.findAllByFeedbackTitleContainsIgnoreCase(eq("abc"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(customerFeedbackMapper.toResponse(entity)).thenReturn(CustomerFeedbackResponse.builder().id(1L).build());

        Page<CustomerFeedbackResponse> result = service.getAllFeedbacks(0, 10, "abc");

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).extracting(CustomerFeedbackResponse::getId).containsExactly(1L);
    }

    @Test
    void getFeedbackById_notFound_throwsEntityNotFound() {
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFeedbackById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void respondToFeedback_responseNotRequired_throwsFeedbackExceptionBeforeSave() {
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).responseRequired(false).build();
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById("u1")).thenReturn(Optional.of(User.builder().id("u1").username("a").passwordHash("x").build()));

        RespondFeedbackRequest request = RespondFeedbackRequest.builder()
                .respondByUserId("u1")
                .responseContent("hi")
                .build();

        assertThatThrownBy(() -> service.respondToFeedback(1L, request))
                .isInstanceOf(FeedbackException.class)
                .satisfies(ex -> assertThat(((FeedbackException) ex).getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_RESPONDING_ERROR));

        verify(customerFeedbackRepository, never()).save(any());
        verify(customerFeedbackMapper, never()).toResponse(any());
    }

    @Test
    void respondToFeedback_happyPath_setsFieldsAndSaves() {
        User responder = User.builder().id("u1").username("responder").passwordHash("x").build();
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).responseRequired(true).build();
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById("u1")).thenReturn(Optional.of(responder));
        when(customerFeedbackRepository.save(any(CustomerFeedback.class))).thenAnswer(inv -> inv.getArgument(0));
        when(customerFeedbackMapper.toResponse(any(CustomerFeedback.class))).thenReturn(CustomerFeedbackResponse.builder().id(1L).build());

        RespondFeedbackRequest request = RespondFeedbackRequest.builder()
                .respondByUserId("u1")
                .responseContent("thanks")
                .build();

        CustomerFeedbackResponse result = service.respondToFeedback(1L, request);

        assertThat(result.getId()).isEqualTo(1L);
        ArgumentCaptor<CustomerFeedback> captor = ArgumentCaptor.forClass(CustomerFeedback.class);
        verify(customerFeedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getRespondedBy()).isEqualTo(responder);
        assertThat(captor.getValue().getResponseContent()).isEqualTo("thanks");
        assertThat(captor.getValue().getRespondedAt()).isNotNull();
    }

    @Test
    void respondToFeedback_saveThrows_wrapsToFeedbackException() {
        User responder = User.builder().id("u1").username("responder").passwordHash("x").build();
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).responseRequired(true).build();
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        when(userRepository.findById("u1")).thenReturn(Optional.of(responder));
        when(customerFeedbackRepository.save(any(CustomerFeedback.class))).thenThrow(new RuntimeException("db"));

        RespondFeedbackRequest request = RespondFeedbackRequest.builder()
                .respondByUserId("u1")
                .responseContent("x")
                .build();

        assertThatThrownBy(() -> service.respondToFeedback(1L, request))
                .isInstanceOf(FeedbackException.class)
                .satisfies(ex -> assertThat(((FeedbackException) ex).getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_RESPONDING_ERROR));
    }

    @Test
    void editingCustomerFeedback_happyPath_updatesRatingsAndSaves() {
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).overallRating(5f).build();
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        UpdatingFeedbackRequest request = UpdatingFeedbackRequest.builder()
                .serviceQualityRating(2)
                .staffBehaviorRating(4)
                .timelinessRating(5)
                .feedbackTitle("new")
                .build();

        service.editingCustomerFeedback(1L, request);

        verify(customerFeedbackMapper).updateEntity(eq(feedback), eq(request));
        ArgumentCaptor<CustomerFeedback> captor = ArgumentCaptor.forClass(CustomerFeedback.class);
        verify(customerFeedbackRepository).save(captor.capture());
        assertThat(captor.getValue().getOverallRating()).isEqualTo((2 + 4 + 5) / 3.0f);
    }

    @Test
    void editingCustomerFeedback_saveThrows_wrapsToFeedbackException() {
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).build();
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        doThrow(new RuntimeException("boom")).when(customerFeedbackMapper).updateEntity(any(), any());

        UpdatingFeedbackRequest request = UpdatingFeedbackRequest.builder()
                .serviceQualityRating(2)
                .staffBehaviorRating(4)
                .timelinessRating(5)
                .build();

        assertThatThrownBy(() -> service.editingCustomerFeedback(1L, request))
                .isInstanceOf(FeedbackException.class)
                .satisfies(ex -> assertThat(((FeedbackException) ex).getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_PERSIST_ERROR));

        verify(customerFeedbackRepository, never()).save(any());
    }

    @Test
    void deleteFeedback_notAdminAndNotOwner_throwsUnauthorized() {
        User other = User.builder().id("u1").username("u").passwordHash("x").build();
        other.getUserRoles().clear();

        User owner = User.builder().id("owner").username("owner").passwordHash("x").build();
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).customer(owner).build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(other));
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        assertThatThrownBy(() -> service.deleteFeedback(1L, "u1"))
                .isInstanceOf(FeedbackException.class)
                .satisfies(ex -> assertThat(((FeedbackException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_FEEDBACK_ACTION));

        verify(customerFeedbackRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteFeedback_admin_canDeleteEvenIfNotOwner() {
        User admin = User.builder().id("admin").username("admin").passwordHash("x").build();
        Role role = Role.builder().roleName(RoleType.ADMIN.name()).build();
        UserRole ur = UserRole.builder().user(admin).role(role).build();
        admin.getUserRoles().add(ur);

        User owner = User.builder().id("owner").username("owner").passwordHash("x").build();
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).customer(owner).build();

        when(userRepository.findById("admin")).thenReturn(Optional.of(admin));
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        service.deleteFeedback(1L, "admin");

        verify(customerFeedbackRepository).deleteById(1L);
    }

    @Test
    void deleteFeedback_owner_canDelete() {
        User owner = User.builder().id("u1").username("u").passwordHash("x").build();
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).customer(owner).build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(owner));
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));

        service.deleteFeedback(1L, "u1");

        verify(customerFeedbackRepository).deleteById(1L);
    }

    @Test
    void deleteFeedback_deleteThrows_wrapsToPersistError() {
        User owner = User.builder().id("u1").username("u").passwordHash("x").build();
        CustomerFeedback feedback = CustomerFeedback.builder().id(1L).customer(owner).build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(owner));
        when(customerFeedbackRepository.findById(1L)).thenReturn(Optional.of(feedback));
        doThrow(new RuntimeException("db")).when(customerFeedbackRepository).deleteById(1L);

        assertThatThrownBy(() -> service.deleteFeedback(1L, "u1"))
                .isInstanceOf(FeedbackException.class)
                .satisfies(ex -> assertThat(((FeedbackException) ex).getErrorCode()).isEqualTo(ErrorCode.FEEDBACK_PERSIST_ERROR));
    }
}
