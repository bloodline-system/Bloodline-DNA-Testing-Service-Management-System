package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.RegisterRequest;
import com.dna_testing_system.dev.dto.request.UpdateProfileRequest;
import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.entity.User;
import com.dna_testing_system.dev.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toEntity(RegisterRequest request);

    /**
     * Maps User entity to UserResponse DTO, including fields from UserProfile
     * @param user The user entity to map
     * @return UserResponse DTO with user and profile information
     */
    @Mapping(target = "userId", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "firstName", source = "userProfile.firstName")
    @Mapping(target = "lastName", source = "userProfile.lastName")
    @Mapping(target = "email", source = "userProfile.email")
    @Mapping(target = "phoneNumber", source = "userProfile.phoneNumber")
    @Mapping(target = "profileImageUrl", source = "userProfile.profileImageUrl")
    @Mapping(target = "dateOfBirth", source = "userProfile.dateOfBirth")
    @Mapping(target = "message", ignore = true)
    UserResponse toResponse(User user);

    /**
     * Maps UpdateProfileRequest to UserProfile entity, updating only non-null fields
     * @param request The update profile request
     * @param userProfile The target UserProfile entity to update
     */
    void updateUserProfileFromDto(UpdateProfileRequest request, @MappingTarget UserProfile userProfile);

    /**
     * Maps UpdateProfileRequest to a new UserProfile entity
     * @param request The update profile request
     * @param user The user entity that owns this profile
     * @return A new UserProfile entity
     */
    @Mapping(target = "profileId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserProfile toUserProfile(UpdateProfileRequest request, User user);
}
