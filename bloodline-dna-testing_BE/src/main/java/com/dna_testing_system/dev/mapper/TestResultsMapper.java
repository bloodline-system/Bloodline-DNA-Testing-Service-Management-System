package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.TestResultsResquest;
import com.dna_testing_system.dev.dto.response.TestResultsResponse;
import com.dna_testing_system.dev.entity.TestResult;
import com.dna_testing_system.dev.enums.ResultStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;

@Mapper(componentModel = "spring")
public interface TestResultsMapper {

    @Mapping(target="reportFilePath", source="testResultsResquest.reportFile")
    TestResult toEntity(TestResultsResquest testResultsResquest);

    @Mapping(target="testResultId", source="testResult.id")
    @Mapping(target="orderId", source="testResult.order.id")
    @Mapping(target="rawTestId", source="testResult.rawData.id")
    @Mapping(target ="staff", source="testResult.analyzedByStaff.username")
    @Mapping(target="testDate", source="testResult.testDate")
    @Mapping(target="resultSummary", source="testResult.resultSummary")
    @Mapping(target="detailedResults", source="testResult.detailedResults")
    @Mapping(target="reportFile", source="testResult.reportFilePath")
    @Mapping(target="resultStatus", source="testResult.resultStatus")
    @Mapping(target="reportGenerated", source="testResult.reportGenerated")
    @Mapping(target="createdAt", source="testResult.createdAt")
    @Mapping(target="updatedAt", source="testResult.updatedAt")
    TestResultsResponse toResponse(TestResult testResult);

    @Mapping(target="reportFilePath", source="testResultsResquest.reportFile")
    @Mapping(target="testDate", source="testResultsResquest.testDate")
    @Mapping(target="resultSummary", source="testResultsResquest.resultSummary")
    @Mapping(target="detailedResults", source="testResultsResquest.detailedResults")
    @Mapping(target="reportGenerated", source="testResultsResquest.reportGenerated")
    @Mapping(target="resultStatus", source="testResultsResquest.resultStatus")
    void updateEntityFromRequest(TestResultsResquest testResultsResquest,@MappingTarget TestResult testResult);
}
