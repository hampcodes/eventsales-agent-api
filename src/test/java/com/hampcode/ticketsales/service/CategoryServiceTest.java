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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    private static final Long ID = 3L;

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    @DisplayName("create guarda la categoria con el nombre sin espacios sobrantes")
    void create_freeName_savesTrimmedAndReturnsDto() {
        given(categoryRepository.existsByNameIgnoreCase("Música")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(ID);
            return c;
        });
        given(categoryMapper.toResponseDTO(any(Category.class)))
                .willAnswer(inv -> new CategoryResponseDTO(((Category) inv.getArgument(0)).getId(),
                        ((Category) inv.getArgument(0)).getName()));

        CategoryResponseDTO result = categoryService.create(new CategoryRequestDTO("  Música  "));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        then(categoryRepository).should().save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Música");
        assertThat(result).isEqualTo(new CategoryResponseDTO(ID, "Música"));
    }

    @Test
    @DisplayName("create lanza DuplicateResourceException si el nombre existe (sin distinguir mayusculas) y no guarda")
    void create_existingName_throwsDuplicateResourceException() {
        given(categoryRepository.existsByNameIgnoreCase("MÚSICA")).willReturn(true);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequestDTO(" MÚSICA ")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Ya existe una categoría con ese nombre");
        then(categoryRepository).should(never()).save(any(Category.class));
    }

    @Test
    @DisplayName("update lanza ResourceNotFoundException si la categoria no existe")
    void update_missingCategory_throwsResourceNotFoundException() {
        given(categoryRepository.findById(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update(ID, new CategoryRequestDTO("Teatro")))
                .isInstanceOf(ResourceNotFoundException.class);
        then(categoryRepository).should(never()).save(any(Category.class));
    }

    @Test
    @DisplayName("update lanza DuplicateResourceException si otra categoria ya usa el nombre")
    void update_nameOfAnotherCategory_throwsDuplicateResourceException() {
        Category existing = Category.builder().id(ID).name("Teatro").build();
        given(categoryRepository.findById(ID)).willReturn(Optional.of(existing));
        given(categoryRepository.existsByNameIgnoreCaseAndIdNot("Música", ID)).willReturn(true);

        assertThatThrownBy(() -> categoryService.update(ID, new CategoryRequestDTO("Música")))
                .isInstanceOf(DuplicateResourceException.class);
        then(categoryRepository).should(never()).save(any(Category.class));
    }

    @Test
    @DisplayName("update con el nombre de la propia categoria es valido")
    void update_ownName_succeeds() {
        Category existing = Category.builder().id(ID).name("Teatro").build();
        given(categoryRepository.findById(ID)).willReturn(Optional.of(existing));
        given(categoryRepository.existsByNameIgnoreCaseAndIdNot("teatro", ID)).willReturn(false);
        given(categoryRepository.save(existing)).willReturn(existing);
        given(categoryMapper.toResponseDTO(existing)).willReturn(new CategoryResponseDTO(ID, "teatro"));

        CategoryResponseDTO result = categoryService.update(ID, new CategoryRequestDTO(" teatro "));

        assertThat(existing.getName()).isEqualTo("teatro");
        assertThat(result.name()).isEqualTo("teatro");
    }

    @Test
    @DisplayName("getById lanza ResourceNotFoundException si no existe")
    void getById_missing_throwsResourceNotFoundException() {
        given(categoryRepository.findById(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoría no encontrada");
    }

    @Test
    @DisplayName("list devuelve la pagina con los campos de paginacion del Page")
    void list_returnsPageResponse() {
        Pageable pageable = PageRequest.of(1, 2);
        Category c = Category.builder().id(ID).name("Teatro").build();
        CategoryResponseDTO dto = new CategoryResponseDTO(ID, "Teatro");
        given(categoryRepository.findAll(pageable)).willReturn(new PageImpl<>(List.of(c), pageable, 3));
        given(categoryMapper.toResponseDTO(c)).willReturn(dto);

        PageResponseDTO<CategoryResponseDTO> result = categoryService.list(pageable);

        assertThat(result.content()).containsExactly(dto);
        assertThat(result.pageNumber()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.last()).isTrue();
    }

    @Test
    @DisplayName("delete lanza ResourceNotFoundException si no existe")
    void delete_missing_throwsResourceNotFoundException() {
        given(categoryRepository.findById(ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(ID)).isInstanceOf(ResourceNotFoundException.class);
        then(categoryRepository).should(never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("delete con eventos asociados lanza DuplicateResourceException y no borra")
    void delete_withEvents_throwsDuplicateResourceException() {
        Category c = Category.builder().id(ID).name("Teatro").build();
        given(categoryRepository.findById(ID)).willReturn(Optional.of(c));
        given(eventRepository.existsByCategoryId(ID)).willReturn(true);

        assertThatThrownBy(() -> categoryService.delete(ID))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("eventos asociados");
        then(categoryRepository).should(never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("delete sin eventos borra la categoria")
    void delete_withoutEvents_deletes() {
        Category c = Category.builder().id(ID).name("Teatro").build();
        given(categoryRepository.findById(ID)).willReturn(Optional.of(c));
        given(eventRepository.existsByCategoryId(ID)).willReturn(false);

        categoryService.delete(ID);

        then(categoryRepository).should().delete(c);
    }
}
