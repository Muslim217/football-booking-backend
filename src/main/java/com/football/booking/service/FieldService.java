package com.football.booking.service;

import com.football.booking.dto.request.FieldRequest;
import com.football.booking.dto.response.FieldResponse;
import com.football.booking.dto.response.TimeSlotResponse;
import com.football.booking.entity.Booking;
import com.football.booking.entity.Field;
import com.football.booking.entity.User;
import com.football.booking.enums.Role;
import com.football.booking.exception.AccessDeniedException;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.BookingRepository;
import com.football.booking.repository.FieldRepository;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FieldService {

    // Рабочие часы площадок: с 08:00 до 23:00, слоты по 1 часу
    private static final LocalTime OPEN_TIME  = LocalTime.of(8, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(23, 0);

    private final FieldRepository fieldRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    // === Публичные методы ===

    public Page<FieldResponse> getAllActiveFields(Pageable pageable) {
        return fieldRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    public FieldResponse getFieldById(Long id) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Поле не найдено с ID: " + id));
        return mapToResponse(field);
    }

    /**
     * Возвращает список слотов (по 1 часу) на указанную дату.
     * Каждый слот помечен: available=true/false
     */
    public List<TimeSlotResponse> getSchedule(Long fieldId, LocalDate date) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Поле не найдено с ID: " + fieldId));

        LocalDateTime dayStart = date.atTime(OPEN_TIME);
        LocalDateTime dayEnd   = date.atTime(CLOSE_TIME);

        // Все активные бронирования на этот день
        List<Booking> bookings = bookingRepository.findConflictingBookings(fieldId, dayStart, dayEnd);

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime cursor = OPEN_TIME;

        while (cursor.isBefore(CLOSE_TIME)) {
            LocalTime slotEnd = cursor.plusHours(1);
            LocalDateTime slotStart_dt = date.atTime(cursor);
            LocalDateTime slotEnd_dt   = date.atTime(slotEnd);

            boolean isBooked = bookings.stream().anyMatch(b ->
                    b.getStartTime().isBefore(slotEnd_dt) &&
                    b.getEndTime().isAfter(slotStart_dt));

            // Прошедшие слоты тоже недоступны
            boolean isPast = slotStart_dt.isBefore(LocalDateTime.now());

            BigDecimal slotPrice = field.getPricePerHour()
                    .setScale(2, RoundingMode.HALF_UP);

            slots.add(TimeSlotResponse.builder()
                    .startTime(cursor)
                    .endTime(slotEnd)
                    .available(!isBooked && !isPast)
                    .price(slotPrice)
                    .build());

            cursor = slotEnd;
        }

        return slots;
    }

    // === Методы владельца (OWNER) ===

    public Page<FieldResponse> getMyFields(Authentication authentication, Pageable pageable) {
        User owner = getAuthenticatedUser(authentication);
        return fieldRepository.findByOwnerId(owner.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public FieldResponse createField(FieldRequest request, Authentication authentication) {
        User owner = getAuthenticatedUser(authentication);

        if (owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только владельцы полей могут создавать поля");
        }

        Field field = Field.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .fieldType(request.getFieldType())
                .pricePerHour(request.getPricePerHour())
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
                .owner(owner)
                .isActive(true)
                .build();

        return mapToResponse(fieldRepository.save(field));
    }

    @Transactional
    public FieldResponse updateField(Long id, FieldRequest request, Authentication authentication) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Поле не найдено с ID: " + id));

        checkOwnership(field, authentication);

        field.setName(request.getName());
        field.setAddress(request.getAddress());
        field.setCity(request.getCity());
        field.setFieldType(request.getFieldType());
        field.setPricePerHour(request.getPricePerHour());
        field.setDescription(request.getDescription());
        if (request.getPhotoUrl() != null) {
            field.setPhotoUrl(request.getPhotoUrl());
        }

        return mapToResponse(fieldRepository.save(field));
    }

    @Transactional
    public void deactivateField(Long id, Authentication authentication) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Поле не найдено с ID: " + id));
        checkOwnership(field, authentication);
        field.setIsActive(false);
        fieldRepository.save(field);
    }

    @Transactional
    public void activateField(Long id, Authentication authentication) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Поле не найдено с ID: " + id));
        checkOwnership(field, authentication);
        field.setIsActive(true);
        fieldRepository.save(field);
    }

    // === Вспомогательные методы ===

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private void checkOwnership(Field field, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = field.getOwner().getId().equals(user.getId());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Вы можете управлять только своими полями");
        }
    }

    private FieldResponse mapToResponse(Field field) {
        return FieldResponse.builder()
                .id(field.getId())
                .name(field.getName())
                .address(field.getAddress())
                .city(field.getCity())
                .fieldType(field.getFieldType())
                .pricePerHour(field.getPricePerHour())
                .description(field.getDescription())
                .photoUrl(field.getPhotoUrl())
                .isActive(field.getIsActive())
                .ownerUsername(field.getOwner().getUsername())
                .createdAt(field.getCreatedAt())
                .build();
    }
}
