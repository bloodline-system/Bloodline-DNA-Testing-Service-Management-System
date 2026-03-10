package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.UpdateProfileRequest;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    // Define methods for mapping UserProfile to UserProfileDTO and vice versa
    // For example:
    @Mapping(target= "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.userProfile.firstName")
    @Mapping(target = "lastName", source = "user.userProfile.lastName")
    @Mapping(target = "email", source = "user.userProfile.email")
    @Mapping(target = "phoneNumber", source = "user.userProfile.phoneNumber")
    @Mapping(target = "profileImageUrl", source = "user.userProfile.profileImageUrl")
    @Mapping(target = "dateOfBirth", source = "user.userProfile.dateOfBirth")
    @Mapping(target = "createdAt", source = "user.userProfile.createdAt")
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target="isActive", source = "user.isActive")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "role", expression = "java(getFirstRoleName(user))")
    UserProfileResponse toDto(User user);
    UserProfile toEntity(UserProfileRequest userProfileDTO);



    @Mapping(target = "userProfile.firstName", source = "firstName")
    @Mapping(target = "userProfile.lastName", source = "lastName")
    @Mapping(target = "userProfile.email", source = "email")
    @Mapping(target = "userProfile.phoneNumber", source = "phoneNumber")
    @Mapping(target = "userProfile.profileImageUrl", source = "profileImageUrl")
    @Mapping(target = "userProfile.dateOfBirth", source = "dateOfBirth")
    void updateUserProfileFromDto(UserProfileRequest request, @MappingTarget UserProfile userProfile);

    default String getFirstRoleName(User user) {
        return user.getUserRoles().stream()
                .findFirst()
                .map(userRole -> userRole.getRole().getRoleName())
                .orElse(null);
    }
}
