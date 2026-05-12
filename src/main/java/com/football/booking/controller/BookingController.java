package com.football.booking.controller;

import com.football.booking.dto.request.BookingRequest;
import com.football.booking.dto.response.BookingResponse;
import com.football.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Бронирования", description = "Управление бронированиями спортивных площадок")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Создать бронирование")
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, authentication));
    }

    @GetMapping("/my")
    @Operation(summary = "Мои бронирования (с пагинацией)")
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            Authentication authentication,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingService.getMyBookings(authentication, pageable));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Все бронирования — только ADMIN (с пагинацией)")
    public ResponseEntity<Page<BookingResponse>> getAllBookings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingService.getAllBookings(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Бронирование по ID")
    public ResponseEntity<BookingResponse> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBookingById(id, authentication));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Отменить бронирование")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, authentication));
    }

    @GetMapping("/owner")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Все брони по полям владельца (OWNER/ADMIN)")
    public ResponseEntity<Page<BookingResponse>> getOwnerBookings(
            Authentication authentication,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        return ResponseEntity.ok(bookingService.getOwnerBookings(authentication, pageable));
    }

    @GetMapping("/field/{fieldId}")
    @Operation(summary = "Бронирования по площадке (с пагинацией)")
    public ResponseEntity<Page<BookingResponse>> getBookingsByField(
            @PathVariable Long fieldId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("startTime").descending());
        return ResponseEntity.ok(bookingService.getBookingsByField(fieldId, pageable));
    }
}
