package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.auth.RegisterRequestDTO;
import com.dna_testing_system.dev.dto.response.auth.AuthTokensResponseDTO;
import com.dna_testing_system.dev.dto.response.auth.RegisterResponseDTO;
import com.dna_testing_system.dev.entity.SignUp;
import com.dna_testing_system.dev.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentVerificationToken", ignore = true)
    @Mapping(target = "expiredVerificationTokenDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SignUp toSignUp(RegisterRequestDTO dto);

    @Mapping(target = "signUpId", source = "id")
    RegisterResponseDTO toRegisterResponseDTO(SignUp signUp);

    @Mapping(target = "userId", source = "id")
    AuthTokensResponseDTO toAuthTokensResponseDTO(User user);
}
