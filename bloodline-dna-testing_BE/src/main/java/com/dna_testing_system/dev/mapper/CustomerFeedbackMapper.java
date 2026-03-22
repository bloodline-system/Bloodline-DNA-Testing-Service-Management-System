package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.CreateFeedbackRequest;
import com.dna_testing_system.dev.dto.request.RespondFeedbackRequest;
import com.dna_testing_system.dev.dto.request.UpdatingFeedbackRequest;
import com.dna_testing_system.dev.dto.request.UpdatingReportRequest;
import com.dna_testing_system.dev.dto.response.CustomerFeedbackResponse;
import com.dna_testing_system.dev.entity.CustomerFeedback;
import com.dna_testing_system.dev.entity.MedicalService;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerFeedbackMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", expression = "java(requestCustomer)")
    @Mapping(target = "service", expression = "java(requestService)")
    @Mapping(target = "order", expression = "java(requestOrder)")
    @Mapping(target = "respondedAt", ignore = true)
    @Mapping(target = "respondedBy", ignore = true)
    @Mapping(target = "responseContent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerFeedback toEntity(
            CreateFeedbackRequest request,
            @Context User requestCustomer,
            @Context MedicalService requestService,
            @Context ServiceOrder requestOrder
    );
    @Mapping(target = "customerName", source = "customer", qualifiedByName = "fullNameFromUser")
    @Mapping(target = "serviceName", source = "service.serviceName")
    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "respondedByName", source = "respondedBy", qualifiedByName = "fullNameFromUser")
    CustomerFeedbackResponse toResponse(CustomerFeedback entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "respondedAt", expression = "java(java.time.LocalDateTime.now())")
    void updateResponse(@MappingTarget CustomerFeedback feedback,
                        @Context User respondedBy,
                        RespondFeedbackRequest request);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget CustomerFeedback feedback, UpdatingFeedbackRequest dto);


    @Named("fullNameFromUser")
    default String mapFullName(User user) {
        if (user == null || user.getUserProfile() == null) return null;
        return user.getUserProfile().getFirstName() + " " + user.getUserProfile().getLastName();
    }
}
