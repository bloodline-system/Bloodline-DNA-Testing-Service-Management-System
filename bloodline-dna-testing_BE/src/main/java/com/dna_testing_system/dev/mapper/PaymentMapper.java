package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.PaymentUpdatingRequest;
import com.dna_testing_system.dev.dto.response.PaymentResponse;
import com.dna_testing_system.dev.entity.Payment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(source = "order.id", target = "orderId")
    @Mapping(source = "order.service.serviceName", target = "serviceName")
    @Mapping(source = "order.customer.userProfile.email", target = "email")
    @Mapping(source = "order.customer.userProfile.phoneNumber", target = "phoneNumber")
    @Mapping(source = "order.customer.userProfile", target = "customerName", qualifiedByName = "mapCustomerName")
    @Mapping(source = "order.collectionAddress", target = "collectionAddress")
    @Mapping(source = "promotion.promotionCode", target = "promotionCode") // can err
    @Mapping(source = "paymentMethod", target = "paymentMethod")
    @Mapping(source = "grossAmount", target = "grossAmount")
    @Mapping(source = "discountAmount", target = "discountAmount")
    @Mapping(source = "netAmount", target = "netAmount")
    @Mapping(source = "paymentStatus", target = "paymentStatus")
    @Mapping(source = "paymentDate", target = "paymentDate")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    PaymentResponse toPaymentResponse(Payment payment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updatePaymentFromRequest(PaymentUpdatingRequest request, @MappingTarget Payment payment);

    @Named("mapCustomerName")
    static String mapCustomerName(com.dna_testing_system.dev.entity.UserProfile profile) {
        if (profile == null) return "";
        String first = profile.getFirstName() != null ? profile.getFirstName() : "";
        String last = profile.getLastName() != null ? profile.getLastName() : "";
        return (first + " " + last).trim();
    }
}
