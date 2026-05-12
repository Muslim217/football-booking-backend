package com.football.booking.service;

import com.football.booking.dto.request.FieldRequest;
import com.football.booking.dto.response.FieldResponse;
import com.football.booking.dto.response.TimeSlotResponse;
import com.football.booking.entity.Booking;
import com.football.booking.entity.Field;
import com.football.booking.entity.User;
import com.football.booking.enums.FieldType;
import com.football.booking.enums.Role;
import com.football.booking.exception.AccessDeniedException;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.BookingRepository;
import com.football.booking.repository.FavoriteRepository;
import com.football.booking.repository.FieldRepository;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    // Рабочие часы: 08:00 – 23:00, слоты по 1 часу
    private static final LocalTime OPEN_TIME  = LocalTime.of(8, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(23, 0);

    private final FieldRepository fieldRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final FavoriteRepository favoriteRepository;
    private final PhotoStorageService photoStorageService;

    // === Публичные методы ===

    public Page<FieldResponse> getAllActiveFields(Pageable pageable, String type, String search,
                                                  Authentication authentication) {
        FieldType fieldTypeFilter = null;
        if (type != null && !type.isBlank()) {
            try {
                fieldTypeFilter = FieldType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                return Page.empty(pageable);
            }
        }

        String searchFilter = (search != null && !search.isBlank()) ? search.trim() : null;

        // Теперь фильтрация полностью на уровне БД через @Query
        Page<Field> fields = fieldRepository.findActiveWithFilters(fieldTypeFilter, searchFilter, pageable);

        Long userId = resolveUserId(authentication);
        return fields.map(f -> mapToResponse(f, userId));
    }

    public FieldResponse getFieldById(Long id, Authentication authentication) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Поле не найдено с ID: " + id));
        Long userId = resolveUserId(authentication);
        return mapToResponse(field, userId);
    }

    /**
     * Расписание на день: список 1-часовых слотов с признаком доступности
     */
    public List<TimeSlotResponse> getSchedule(Long fieldId, LocalDate date) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Поле не найдено с ID: " + fieldId));

        LocalDateTime dayStart = date.atTime(OPEN_TIME);
        LocalDateTime dayEnd   = date.atTime(CLOSE_TIME);

        List<Booking> bookings = bookingRepository.findConflictingBookings(fieldId, dayStart, dayEnd);

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime cursor = OPEN_TIME;
        LocalDateTime now = LocalDateTime.now();

        while (cursor.isBefore(CLOSE_TIME)) {
            LocalTime slotEnd = cursor.plusHours(1);
            LocalDateTime slotStartDt = date.atTime(cursor);
            LocalDateTime slotEndDt   = date.atTime(slotEnd);

            boolean isBooked = bookings.stream().anyMatch(b ->
                    b.getStartTime().isBefore(slotEndDt) && b.getEndTime().isAfter(slotStartDt));
            boolean isPast   = slotStartDt.isBefore(now);

            BigDecimal slotPrice = field.getPricePerHour().setScale(2, RoundingMode.HALF_UP);

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
                .map(f -> mapToResponse(f, owner.getId()));
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

        return mapToResponse(fieldRepository.save(field), owner.getId());
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

        User user = getAuthenticatedUser(authentication);
        return mapToResponse(fieldRepository.save(field), user.getId());
    }

    @Transactional
    public FieldResponse uploadPhoto(Long id, MultipartFile file, Authentication authentication) {
        Field field = fieldRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Поле не найдено с ID: " + id));

        checkOwnership(field, authentication);

        // Удаляем старое фото если есть
        if (field.getPhotoUrl() != null) {
            photoStorageService.delete(field.getPhotoUrl());
        }

        String photoUrl = photoStorageService.store(file);
        field.setPhotoUrl(photoUrl);

        User user = getAuthenticatedUser(authentication);
        return mapToResponse(fieldRepository.save(field), user.getId());
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

    // === Вспомогательные ===

    /**
     * Публичный маппер без персонализации — используется FavoriteService и другими сервисами
     */
    public FieldResponse mapToResponsePublic(Field field) {
        return mapToResponse(field, null);
    }

    private FieldResponse mapToResponse(Field field, Long currentUserId) {
        boolean isFavorite = currentUserId != null &&
                favoriteRepository.existsByUserIdAndFieldId(currentUserId, field.getId());

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
                .avgRating(field.getAvgRating())
                .reviewCount(field.getReviewCount())
                .favoriteCount(field.getFavoriteCount())
                .isFavorite(isFavorite)
                .ownerVerificationStatus(field.getOwner().getVerificationStatus())
                .build();
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        return userRepository.findByUsername(authentication.getName())
                .map(User::getId)
                .orElse(null);
    }

    private void checkOwnership(Field field, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);
        boolean isAdmin = user.getRole() == Role.ADMIN;
        boolean isOwner = field.getOwner().getId().equals(user.getId());
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("Вы можете управлять только своими полями");
        }
    }
}
