package com.football.booking.repository;

import com.football.booking.entity.Field;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<Field, Long> {

    // Без пагинации (для внутреннего использования)
    List<Field> findByIsActiveTrue();

    List<Field> findByOwnerId(Long ownerId);

    // С пагинацией (для API)
    Page<Field> findByIsActiveTrue(Pageable pageable);

    Page<Field> findByOwnerId(Long ownerId, Pageable pageable);
}
