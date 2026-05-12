package com.football.booking.repository;

import com.football.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Без пагинации (для логики расписания)
    List<Booking> findByUserId(Long userId);
    List<Booking> findByFieldId(Long fieldId);

    // С пагинацией (для API)
    Page<Booking> findByUserId(Long userId, Pageable pageable);
    Page<Booking> findByFieldId(Long fieldId, Pageable pageable);
    Page<Booking> findByFieldIdIn(List<Long> fieldIds, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.field.id = :fieldId " +
           "AND b.status <> 'CANCELLED' " +
           "AND b.startTime < :endTime " +
           "AND b.endTime > :startTime")
    List<Booking> findConflictingBookings(
            @Param("fieldId") Long fieldId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
