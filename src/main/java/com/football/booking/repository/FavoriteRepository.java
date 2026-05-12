package com.football.booking.repository;

import com.football.booking.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByUserId(Long userId);

    Optional<Favorite> findByUserIdAndFieldId(Long userId, Long fieldId);

    boolean existsByUserIdAndFieldId(Long userId, Long fieldId);

    void deleteByUserIdAndFieldId(Long userId, Long fieldId);
}
