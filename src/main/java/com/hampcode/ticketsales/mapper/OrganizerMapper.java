package com.hampcode.ticketsales.mapper;

import com.hampcode.ticketsales.dto.request.OrganizerRequestDTO;
import com.hampcode.ticketsales.dto.response.OrganizerResponseDTO;
import com.hampcode.ticketsales.entity.Organizer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizerMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    Organizer toEntity(OrganizerRequestDTO request);

    OrganizerResponseDTO toResponseDTO(Organizer organizer);
}
