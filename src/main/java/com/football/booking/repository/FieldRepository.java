package com.football.booking.repository;

import com.football.booking.entity.Field;
import com.football.booking.enums.FieldType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<Field, Long> {

    // Базовые
    List<Field> findByIsActiveTrue();
    List<Field> findByOwnerId(Long ownerId);

    // С пагинацией
    Page<Field> findByIsActiveTrue(Pageable pageable);
    Page<Field> findByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Активные площадки с фильтрацией по типу и/или строке поиска по имени/адресу.
     * Все параметры опциональные — null значит "без фильтра".
     */
    @Query("SELECT f FROM Field f WHERE f.isActive = true " +
           "AND (:type IS NULL OR f.fieldType = :type) " +
           "AND (:search IS NULL OR " +
           "     LOWER(f.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(f.address) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(f.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Field> findActiveWithFilters(
            @Param("type") FieldType type,
            @Param("search") String search,
            Pageable pageable);
}
