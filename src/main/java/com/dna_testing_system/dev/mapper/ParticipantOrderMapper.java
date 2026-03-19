package com.dna_testing_system.dev.mapper;

import com.dna_testing_system.dev.dto.request.ParticipantRequest;
import com.dna_testing_system.dev.dto.response.OrderParticipantResponse;
import com.dna_testing_system.dev.entity.OrderParticipant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ParticipantOrderMapper {
    @Mapping(target = "dateOfBirth", source = "birthDate")
    OrderParticipant toEntity(ParticipantRequest orderParticipant);
    @Mapping(target = "dateBirth", source = "dateOfBirth")
    OrderParticipantResponse toDto(OrderParticipant orderParticipant);
    void updateEntityFromDto(ParticipantRequest orderParticipant,@MappingTarget OrderParticipant orderParticipantEntity);
}
