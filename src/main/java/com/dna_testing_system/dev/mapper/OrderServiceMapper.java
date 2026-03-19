package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.ServiceOrderRequestByCustomer;
import com.dna_testing_system.dev.dto.response.ServiceOrderByCustomerResponse;
import com.dna_testing_system.dev.entity.ServiceOrder;
import com.dna_testing_system.dev.service.OrderService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderServiceMapper {
    @Mapping(target = "appointmentDate", source = "appointmentDate")
    ServiceOrder toOrderService(ServiceOrderRequestByCustomer serviceOrderRequestByCustomer);
    ServiceOrderRequestByCustomer toServiceOrderRequestByCustomer(ServiceOrder orderService);
    void updateOrderServiceFromRequest(ServiceOrderRequestByCustomer serviceOrderRequestByCustomer
            ,@MappingTarget ServiceOrder orderService);
    @Mapping(target = "idServiceOrder", source = "orderService.id")
    @Mapping(target = "serviceId", source = "orderService.service.id")
    @Mapping(target="finalAmount", source = "orderService.payments.netAmount")
    @Mapping(target = "orderStatus", source = "orderService.orderStatus")
    @Mapping(target = "medicalServiceName", source = "orderService.service.serviceName")
    @Mapping(target = "appointmentDate", source = "orderService.appointmentDate")
    @Mapping(target = "collectionType", source = "orderService.collectionType")
    @Mapping(target = "collectionAddress", source = "orderService.collectionAddress")
    @Mapping(target = "payments", source = "orderService.payments")
    ServiceOrderByCustomerResponse toServiceOrderByCustomerResponse(ServiceOrder orderService);
    List<ServiceOrderByCustomerResponse> toServiceOrderByCustomerResponseList(List<ServiceOrder> orderServices);
}
