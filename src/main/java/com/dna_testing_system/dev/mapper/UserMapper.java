package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.response.UserResponse;
import com.dna_testing_system.dev.entity.SignUp;
import com.dna_testing_system.dev.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = UserProfileMapper.class
)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", source = "password")
    @Mapping(target = "isActive", expression = "java(true)")
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "profile", source = ".")
    User toUser(SignUp signUp);


    @Mapping(target = "userId", source = "id")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "firstName", source = "profile.firstName")
    @Mapping(target = "lastName", source = "profile.lastName")
    @Mapping(target = "email", source = "profile.email")
    @Mapping(target = "phoneNumber", source = "profile.phoneNumber")
    @Mapping(target = "profileImageUrl", source = "profile.profileImageUrl")
    @Mapping(target = "dateOfBirth", source = "profile.dateOfBirth")
    @Mapping(target = "message", ignore = true)
    UserResponse toResponse(User user);
}