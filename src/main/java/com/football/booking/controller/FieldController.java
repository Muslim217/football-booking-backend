package com.football.booking.controller;

import com.football.booking.dto.request.FieldRequest;
import com.football.booking.dto.response.FieldResponse;
import com.football.booking.dto.response.TimeSlotResponse;
import com.football.booking.service.FieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/fields")
@RequiredArgsConstructor
@Tag(name = "Площадки", description = "Управление спортивными площадками")
public class FieldController {

    private final FieldService fieldService;

    @GetMapping
    @Operation(summary = "Все активные площадки (с пагинацией и фильтрацией)")
    public ResponseEntity<Page<FieldResponse>> getAllActiveFields(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(fieldService.getAllActiveFields(pageable, type, search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Площадка по ID")
    public ResponseEntity<FieldResponse> getFieldById(@PathVariable Long id) {
        return ResponseEntity.ok(fieldService.getFieldById(id));
    }

    @GetMapping("/{id}/schedule")
    @Operation(summary = "Расписание площадки на день — список слотов с доступностью")
    public ResponseEntity<List<TimeSlotResponse>> getSchedule(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(fieldService.getSchedule(id, date));
    }

    @GetMapping("/my")
    @Operation(summary = "Мои площадки (для OWNER)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Page<FieldResponse>> getMyFields(
            Authentication authentication,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(fieldService.getMyFields(authentication, pageable));
    }

    @PostMapping
    @Operation(summary = "Создать площадку (OWNER/ADMIN)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<FieldResponse> createField(
            @Valid @RequestBody FieldRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fieldService.createField(request, authentication));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Обновить площадку")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<FieldResponse> updateField(
            @PathVariable Long id,
            @Valid @RequestBody FieldRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(fieldService.updateField(id, request, authentication));
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Деактивировать площадку")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deactivateField(@PathVariable Long id, Authentication authentication) {
        fieldService.deactivateField(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Активировать площадку")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> activateField(@PathVariable Long id, Authentication authentication) {
        fieldService.activateField(id, authentication);
        return ResponseEntity.noContent().build();
    }
}
