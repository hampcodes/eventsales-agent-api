package com.hampcode.ticketsales.mapper;

import com.hampcode.ticketsales.dto.request.EventRequestDTO;
import com.hampcode.ticketsales.dto.response.EventResponseDTO;
import com.hampcode.ticketsales.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizer", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "availableCapacity", source = "capacityMax")
    Event toEntity(EventRequestDTO request);

    @Mapping(target = "organizerName", source = "organizer.name")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    EventResponseDTO toResponseDTO(Event event);
}
