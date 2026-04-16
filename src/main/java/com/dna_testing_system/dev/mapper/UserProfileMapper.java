package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.UpdateProfileRequest;
import com.dna_testing_system.dev.dto.request.UserProfileRequest;
import com.dna_testing_system.dev.dto.response.UserProfileResponse;
import com.dna_testing_system.dev.dto.response.profile.ProfileResponse;
import com.dna_testing_system.dev.entity.SignUp;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    @Mapping(target = "profileId", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserProfile toProfile(SignUp signUp);

    @Mapping(target = "profileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toUserProfile(UpdateProfileRequest request, User user);

    UserProfile toEntity(UserProfileRequest userProfileDTO);

    @Mapping(target= "userId", source = "id")
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "email", source = "profile.email")
    @Mapping(target = "phoneNumber", source = "profile.phoneNumber")
    @Mapping(target = "profileImageUrl", source = "profile.profileImageUrl")
    @Mapping(target = "dateOfBirth", source = "profile.dateOfBirth")
    @Mapping(target = "createdAt", source = "profile.createdAt")
    @Mapping(target = "message", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", expression = "java(getFirstRoleName(user))")
    UserProfileResponse toDto(User user);

    @Mapping(target= "userId", source = "id")
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "email", source = "profile.email")
    @Mapping(target = "phoneNumber", source = "profile.phoneNumber")
    @Mapping(target = "profileImageUrl", source = "profile.profileImageUrl")
    @Mapping(target = "dateOfBirth", source = "profile.dateOfBirth")
    @Mapping(target = "role", expression = "java(getFirstRoleName(user))")
    ProfileResponse toProfileResponse(User user);

    void updateUserProfileFromDto(UpdateProfileRequest request, @MappingTarget UserProfile userProfile);
    @Mapping(target = "email", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserProfileFromDto(UserProfileRequest request, @MappingTarget UserProfile userProfile);

    default String getFirstRoleName(User user) {
        return user.getUserRoles().stream()
                .findFirst()
                .map(userRole -> userRole.getRole().getRoleName())
                .orElse(null);
    }
}