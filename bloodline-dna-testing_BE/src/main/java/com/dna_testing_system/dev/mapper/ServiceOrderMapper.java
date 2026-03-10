package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.response.ServiceOrderResponse;
import com.dna_testing_system.dev.entity.*;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.util.Set;


@Mapper(componentModel = "spring")
public interface ServiceOrderMapper {

    @Mapping(target = "customerName", expression = "java(getFullName(order.getCustomer()))")
    @Mapping(source = "customer.username", target = "username")
    @Mapping(source = "customer.userProfile.profileImageUrl", target = "profileImageUrl")
    @Mapping(source = "customer.userProfile.email", target = "email")
    @Mapping(source = "customer.userProfile.phoneNumber", target = "phoneNumber")
    @Mapping(source = "service.serviceName", target = "serviceName")
    @Mapping(source = "service.serviceType.typeName", target = "serviceType")
    @Mapping(source = "service.serviceCategory.description", target = "serviceCategory")
    @Mapping(source = "service.participants", target = "participants")
    @Mapping(source = "service.executionTimeDays", target = "executionTimeDays")
    @Mapping(source = "orderKits", target = "orderKits")
    @Mapping(source = "orderParticipants", target = "orderParticipants")
    @Mapping(source = "sampleCollections", target = "sampleCollections")
    @Mapping(target = "collectionStaffName", expression = "java(getCollectionStaffName(order))")
    @Mapping(target = "collectionStaffEmail", expression = "java(getCollectionStaffEmail(order))")
    @Mapping(target = "collectionStaffPhone", expression = "java(getCollectionStaffPhone(order))")
    @Mapping(target = "analysisStaffName", expression = "java(getAnalysisStaffName(order))")
    @Mapping(target = "analysisStaffEmail", expression = "java(getAnalysisStaffEmail(order))")
    @Mapping(target = "analysisStaffPhone", expression = "java(getAnalysisStaffPhone(order))")
    @Mapping(target = "testingResultStatus", expression = "java(getTestResultStatus(order))")
    @Mapping(target = "testingResultSummary", expression = "java(getTestResultSummary(order))")
    @Mapping(target = "testingResultAnalyzedBy", expression = "java(getTestResultAnalyzedBy(order))")
    @Mapping(target = "testingResultAnalysisDate", expression = "java(getTestResultAnalysisDate(order))")
    @Mapping(target = "detailedResults", source = "testResults", qualifiedByName = "mapDetailedResults")
    @Mapping(target = "rawData", source = "testResults", qualifiedByName = "mapRawData")
    @Mapping(target = "totalAmount", source = "payments.grossAmount")
    @Mapping(target = "discountAmount", source = "payments.discountAmount")
    @Mapping(target = "finalAmount", source = "payments.netAmount")
    @Mapping(target = "paymentStatus", source = "payments.paymentStatus")
    ServiceOrderResponse toDto(ServiceOrder order);

    default String getFullName(User user) {
        if (user != null && user.getUserProfile() != null) {
            return user.getUserProfile().getFirstName() + " " + user.getUserProfile().getLastName();
        }
        return null;
    }

    // SampleCollection (lấy bản đầu tiên)
    default SampleCollection getFirstSampleCollection(ServiceOrder order) {
        return order.getSampleCollections().stream().findFirst().orElse(null);
    }

    default String getCollectionStaffName(ServiceOrder order) {
        var sc = getFirstSampleCollection(order);
        return (sc != null && sc.getStaff() != null) ? getFullName(sc.getStaff()) : null;
    }

    default String getCollectionStaffEmail(ServiceOrder order) {
        var sc = getFirstSampleCollection(order);
        return (sc != null && sc.getStaff() != null) ? sc.getStaff().getUserProfile().getEmail() : null;
    }

    default String getCollectionStaffPhone(ServiceOrder order) {
        var sc = getFirstSampleCollection(order);
        return (sc != null && sc.getStaff() != null && sc.getStaff().getUserProfile() != null)
                ? sc.getStaff().getUserProfile().getPhoneNumber() : null;
    }

    // TestResult (lấy bản đầu tiên)
    default TestResult getFirstTestResult(ServiceOrder order) {
        return order.getTestResults().stream().findFirst().orElse(null);
    }

    default String getAnalysisStaffName(ServiceOrder order) {
        var tr = getFirstTestResult(order);
        return (tr != null && tr.getAnalyzedByStaff() != null) ? getFullName(tr.getAnalyzedByStaff()) : null;
    }

    default String getAnalysisStaffEmail(ServiceOrder order) {
        var tr = getFirstTestResult(order);
        return (tr != null && tr.getAnalyzedByStaff() != null) ? tr.getAnalyzedByStaff().getUserProfile().getEmail() : null;
    }

    default String getAnalysisStaffPhone(ServiceOrder order) {
        var tr = getFirstTestResult(order);
        return (tr != null && tr.getAnalyzedByStaff() != null && tr.getAnalyzedByStaff().getUserProfile() != null)
                ? tr.getAnalyzedByStaff().getUserProfile().getPhoneNumber() : null;
    }

    default String getTestResultStatus(ServiceOrder order) {
        var tr = getFirstTestResult(order);
        return (tr != null && tr.getResultStatus() != null) ? tr.getResultStatus().name() : null;
    }

    default String getTestResultSummary(ServiceOrder order) {
        var tr = getFirstTestResult(order);
        return (tr != null) ? tr.getResultSummary() : null;
    }

    default String getTestResultAnalyzedBy(ServiceOrder order) {
        var tr = getFirstTestResult(order);
        return (tr != null && tr.getAnalyzedByStaff() != null) ? getFullName(tr.getAnalyzedByStaff()) : null;
    }

    default LocalDateTime getTestResultAnalysisDate(ServiceOrder order) {
        var tr = getFirstTestResult(order);
        return (tr != null) ? tr.getTestDate() : null;
    }
    @Named("mapDetailedResults")
    default String mapDetailedResults(Set<TestResult> testResults) {
        return testResults.stream()
                .findFirst()
                .map(TestResult::getDetailedResults)
                .orElse(null);
    }

    @Named("mapRawData")
    default RawTestData mapRawData(Set<TestResult> testResults) {
        return testResults.stream()
                .findFirst()
                .map(TestResult::getRawData)
                .orElse(null);
    }

}
