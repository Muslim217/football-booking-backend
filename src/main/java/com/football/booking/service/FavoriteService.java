package com.football.booking.service;

import com.football.booking.dto.response.FieldResponse;
import com.football.booking.entity.Favorite;
import com.football.booking.entity.Field;
import com.football.booking.entity.User;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.FavoriteRepository;
import com.football.booking.repository.FieldRepository;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FieldRepository fieldRepository;
    private final UserRepository userRepository;
    private final FieldService fieldService;

    @Transactional
    public void addFavorite(Long fieldId, Authentication authentication) {
        User user = getUser(authentication);
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Площадка не найдена с ID: " + fieldId));

        if (favoriteRepository.existsByUserIdAndFieldId(user.getId(), fieldId)) {
            return; // уже в избранном — идемпотентно
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .field(field)
                .build();
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long fieldId, Authentication authentication) {
        User user = getUser(authentication);
        favoriteRepository.deleteByUserIdAndFieldId(user.getId(), fieldId);
    }

    public List<FieldResponse> getMyFavorites(Authentication authentication) {
        User user = getUser(authentication);
        return favoriteRepository.findByUserId(user.getId()).stream()
                .map(f -> {
                    FieldResponse response = fieldService.mapToResponsePublic(f.getField());
                    response.setIsFavorite(true);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public boolean isFavorite(Long fieldId, Authentication authentication) {
        User user = getUser(authentication);
        return favoriteRepository.existsByUserIdAndFieldId(user.getId(), fieldId);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
