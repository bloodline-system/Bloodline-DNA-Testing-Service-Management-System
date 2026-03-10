package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.RawDataRequest;
import com.dna_testing_system.dev.dto.response.RawDataResponse;
import com.dna_testing_system.dev.entity.RawTestData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
@Mapper(componentModel = "spring")
public interface RawDataMapper {
    @Mapping(target="dataFormat", source="rawDataRequest.dataFormat")
    @Mapping(target="rawDataContent", source="rawDataRequest.rawDataContent")
    @Mapping(target="filePath", source="rawDataRequest.file")
    @Mapping(target="collectedAt", source="rawDataRequest.collectionDate")
    RawTestData toEntity(RawDataRequest rawDataRequest);

    @Mapping(target="rawDataId", source="rawTestData.id")
    @Mapping(target="dataFormat", source="rawTestData.dataFormat")
    @Mapping(target="rawDataContent", source="rawTestData.rawDataContent")
    @Mapping(target="filePath", source="rawTestData.filePath")
    RawDataResponse toResponse(RawTestData rawTestData);

    @Mapping(target="dataFormat", source="rawDataRequest.dataFormat")
    @Mapping(target="rawDataContent", source="rawDataRequest.rawDataContent")
    @Mapping(target="filePath", source="rawDataRequest.file")
    void updateEntityFromRequest(RawDataRequest rawDataRequest,@MappingTarget RawTestData rawTestData);
}
