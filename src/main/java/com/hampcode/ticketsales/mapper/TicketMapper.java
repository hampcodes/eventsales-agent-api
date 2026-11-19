package com.hampcode.ticketsales.mapper;

import com.hampcode.ticketsales.dto.response.TicketResponseDTO;
import com.hampcode.ticketsales.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TicketMapper {
    @Mapping(target = "eventName", source = "event.name")
    @Mapping(target = "participantName", source = "participant.name")
    TicketResponseDTO toResponseDTO(Ticket ticket);
}
