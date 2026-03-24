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
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "generatedByUser.username", target = "generatedByUserName")
    @Mapping(source = "generatedByUser.profile.email", target = "generatedByUserEmail")
    @Mapping(source = "generatedByUser.profile.profileImageUrl", target = "generatedByUserImageUrl")
    @Mapping(expression = "java(getPrimaryRoleName(entity.getGeneratedByUser()))", target = "generatedByUserRole")
    SystemReportResponse toResponse(SystemReport entity);

    default String getPrimaryRoleName(User user) {
        if (user == null || user.getUserRoles() == null) {
            return "UNKNOWN";
        }
        return user.getUserRoles().stream()
                .filter(userRole -> userRole != null && Boolean.TRUE.equals(userRole.getIsActive()))
                .map(userRole -> {
                    if (userRole.getRole() == null || userRole.getRole().getRoleName() == null) {
                        return "UNKNOWN";
                    }
                    return userRole.getRole().getRoleName();
                })
                .filter(roleName -> roleName != null && !roleName.isBlank())
                .findFirst()
                .orElse("UNKNOWN");
    }
}
