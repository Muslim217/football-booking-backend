package com.football.booking.controller;

import com.football.booking.dto.response.FieldResponse;
import com.football.booking.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Избранное", description = "Управление избранными площадками")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    @Operation(summary = "Мои избранные площадки")
    public ResponseEntity<List<FieldResponse>> getMyFavorites(Authentication authentication) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(authentication));
    }

    @PostMapping("/{fieldId}")
    @Operation(summary = "Добавить площадку в избранное")
    public ResponseEntity<Void> addFavorite(@PathVariable Long fieldId, Authentication authentication) {
        favoriteService.addFavorite(fieldId, authentication);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{fieldId}")
    @Operation(summary = "Удалить площадку из избранного")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long fieldId, Authentication authentication) {
        favoriteService.removeFavorite(fieldId, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{fieldId}/check")
    @Operation(summary = "Проверить — площадка в избранном?")
    public ResponseEntity<Boolean> isFavorite(@PathVariable Long fieldId, Authentication authentication) {
        return ResponseEntity.ok(favoriteService.isFavorite(fieldId, authentication));
    }
}
