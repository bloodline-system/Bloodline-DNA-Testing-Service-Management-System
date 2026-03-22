package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.NewReportRequest;
import com.dna_testing_system.dev.dto.request.UpdatingReportRequest;
import com.dna_testing_system.dev.dto.response.SystemReportResponse;
import com.dna_testing_system.dev.entity.SystemReport;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserRole;
import org.mapstruct.*;
@Mapper(componentModel = "spring")
public interface SystemReportMapper {
    SystemReport toEntity(NewReportRequest dto);

    // Update: from UpdatingReportRequest → Entity
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget SystemReport entity, UpdatingReportRequest dto);

    // Response: from Entity → Response DTO
    @Mapping(source = "id", target = "reportId")
    @Mapping(source = "generatedByUser.fullName", target = "generatedByUserName")
    @Mapping(source = "generatedByUser.userProfile.email", target = "generatedByUserEmail")
    @Mapping(source = "generatedByUser.userProfile.profileImageUrl", target = "generatedByUserImageUrl")
    @Mapping(expression = "java(getPrimaryRoleName(entity.getGeneratedByUser()))", target = "generatedByUserRole")
    SystemReportResponse toResponse(SystemReport entity);

    default String getPrimaryRoleName(User user) {
        return user.getUserRoles().stream()
                .filter(UserRole::getIsActive)
                .map(userRole -> userRole.getRole().getRoleName()) // nếu Role.name là Enum
                .findFirst()
                .orElse("UNKNOWN");
    }
}
