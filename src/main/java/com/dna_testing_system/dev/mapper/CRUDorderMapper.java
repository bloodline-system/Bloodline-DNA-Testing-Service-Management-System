package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.response.CRUDorderResponse;
import com.dna_testing_system.dev.entity.ServiceOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CRUDorderMapper {
    @Mapping(target = "idServiceOrder", source = "id")
    @Mapping(target="finalAmount", source = "payments.netAmount")
    @Mapping(target = "orderStatus", source = "orderStatus")
    @Mapping(target = "userName", source = "customer.username")
    @Mapping(target = "userEmail", source = "customer.profile.email")
    @Mapping(target ="userPhoneNumber", source = "customer.profile.phoneNumber")
    @Mapping(target = "medicalServiceName", source = "service.serviceName")
    @Mapping(target = "appointmentDate", source = "appointmentDate")
    @Mapping(target = "collectionType", source = "collectionType")
    @Mapping(target = "collectionAddress", source = "collectionAddress")
    CRUDorderResponse toCRUDorderResponse(ServiceOrder orderService);
}
