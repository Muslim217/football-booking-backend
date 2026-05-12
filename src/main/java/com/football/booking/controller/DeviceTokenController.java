package com.football.booking.controller;

import com.football.booking.entity.DeviceToken;
import com.football.booking.entity.User;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.DeviceTokenRepository;
import com.football.booking.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/device-tokens")
@RequiredArgsConstructor
@Tag(name = "Push-уведомления", description = "Регистрация токенов устройств для push")
@SecurityRequirement(name = "bearerAuth")
public class DeviceTokenController {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    /** Зарегистрировать токен устройства (вызывается при запуске приложения) */
    @PostMapping
    @Operation(summary = "Зарегистрировать токен устройства")
    public ResponseEntity<Void> register(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String token    = body.getOrDefault("token", "").trim();
        String platform = body.getOrDefault("platform", "IOS").toUpperCase();

        if (token.isBlank()) return ResponseEntity.badRequest().build();

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        // Idempotent — если токен уже есть, просто обновляем user-а
        deviceTokenRepository.findByToken(token).ifPresentOrElse(
                existing -> { /* уже зарегистрирован */ },
                () -> deviceTokenRepository.save(
                        DeviceToken.builder()
                                .user(user)
                                .token(token)
                                .platform(platform)
                                .build()));

        return ResponseEntity.ok().build();
    }

    /** Удалить токен (вызывается при выходе из аккаунта) */
    @DeleteMapping
    @Operation(summary = "Удалить токен устройства (logout)")
    public ResponseEntity<Void> unregister(
            @RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "").trim();
        if (!token.isBlank()) {
            deviceTokenRepository.deleteByToken(token);
        }
        return ResponseEntity.noContent().build();
    }
}
