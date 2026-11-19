package com.hampcode.ticketsales.service;

import com.hampcode.ticketsales.dto.request.CategoryRequestDTO;
import com.hampcode.ticketsales.dto.response.CategoryResponseDTO;
import com.hampcode.ticketsales.dto.response.PageResponseDTO;
import com.hampcode.ticketsales.entity.Category;
import com.hampcode.ticketsales.exception.DuplicateResourceException;
import com.hampcode.ticketsales.exception.ResourceNotFoundException;
import com.hampcode.ticketsales.mapper.CategoryMapper;
import com.hampcode.ticketsales.repository.CategoryRepository;
import com.hampcode.ticketsales.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Ya existe una categoría con ese nombre");
        }
        Category category = Category.builder().name(name).build();
        return categoryMapper.toResponseDTO(categoryRepository.save(category));
    }

    public PageResponseDTO<CategoryResponseDTO> list(Pageable pageable) {
        Page<Category> page = categoryRepository.findAll(pageable);
        return new PageResponseDTO<>(
                page.getContent().stream().map(categoryMapper::toResponseDTO).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), page.isLast());
    }

    public CategoryResponseDTO getById(Long id) {
        return categoryMapper.toResponseDTO(findOrThrow(id));
    }

    @Transactional
    public CategoryResponseDTO update(Long id, CategoryRequestDTO request) {
        Category category = findOrThrow(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("Ya existe una categoría con ese nombre");
        }
        category.setName(name);
        return categoryMapper.toResponseDTO(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = findOrThrow(id);
        if (eventRepository.existsByCategoryId(id)) {
            throw new DuplicateResourceException("No se puede eliminar la categoría porque tiene eventos asociados");
        }
        categoryRepository.delete(category);
    }

    private Category findOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
    }
}
