package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.response.CRUDorderResponse;
import com.dna_testing_system.dev.entity.ServiceOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CRUDorderMapper {
    @Mapping(target = "idServiceOrder", source = "orderService.id")
    @Mapping(target="finalAmount", source = "orderService.payments.netAmount")
    @Mapping(target = "orderStatus", source = "orderService.orderStatus")
    @Mapping(target = "userName", source = "orderService.customer.username")
    @Mapping(target = "userEmail", source = "orderService.customer.userProfile.email")
    @Mapping(target ="userPhoneNumber", source = "orderService.customer.userProfile.phoneNumber")
    @Mapping(target = "medicalServiceName", source = "orderService.service.serviceName")
    @Mapping(target = "appointmentDate", source = "orderService.appointmentDate")
    @Mapping(target = "collectionType", source = "orderService.collectionType")
    @Mapping(target = "collectionAddress", source = "orderService.collectionAddress")
    CRUDorderResponse toCRUDorderResponse(ServiceOrder orderService);
}
