package com.hampcode.ticketsales.mapper;

import com.hampcode.ticketsales.dto.response.CategoryResponseDTO;
import com.hampcode.ticketsales.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponseDTO toResponseDTO(Category category);
}
