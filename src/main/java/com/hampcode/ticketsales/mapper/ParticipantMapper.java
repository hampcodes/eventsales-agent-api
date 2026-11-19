package com.hampcode.ticketsales.mapper;

import com.hampcode.ticketsales.dto.request.ParticipantRequestDTO;
import com.hampcode.ticketsales.dto.response.ParticipantResponseDTO;
import com.hampcode.ticketsales.entity.Participant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ParticipantMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Participant toEntity(ParticipantRequestDTO request);

    ParticipantResponseDTO toResponseDTO(Participant participant);
}
