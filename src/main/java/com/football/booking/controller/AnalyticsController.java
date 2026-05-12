package com.football.booking.controller;

import com.football.booking.dto.response.OwnerAnalyticsResponse;
import com.football.booking.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Аналитика", description = "Статистика и доходы для владельцев")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/owner")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Аналитика по моим площадкам (OWNER/ADMIN)")
    public ResponseEntity<OwnerAnalyticsResponse> getOwnerAnalytics(Authentication authentication) {
        return ResponseEntity.ok(analyticsService.getOwnerAnalytics(authentication));
    }
}
