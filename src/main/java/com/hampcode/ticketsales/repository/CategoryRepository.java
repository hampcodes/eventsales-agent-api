package com.hampcode.ticketsales.repository;

import com.hampcode.ticketsales.dto.response.CategoryCountResponseDTO;
import com.hampcode.ticketsales.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Query("""
            SELECT new com.hampcode.ticketsales.dto.response.CategoryCountResponseDTO(c.id, c.name, COUNT(e))
            FROM Category c LEFT JOIN Event e ON e.category = c AND e.date >= :today
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    List<CategoryCountResponseDTO> findAllWithUpcomingEventCount(@Param("today") LocalDate today);
}
