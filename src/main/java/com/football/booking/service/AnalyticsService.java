package com.football.booking.service;

import com.football.booking.dto.response.OwnerAnalyticsResponse;
import com.football.booking.entity.Booking;
import com.football.booking.entity.Field;
import com.football.booking.entity.User;
import com.football.booking.enums.BookingStatus;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.BookingRepository;
import com.football.booking.repository.FieldRepository;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BookingRepository bookingRepository;
    private final FieldRepository fieldRepository;
    private final UserRepository userRepository;

    public OwnerAnalyticsResponse getOwnerAnalytics(Authentication authentication) {
        User owner = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        List<Field> fields = fieldRepository.findByOwnerId(owner.getId());

        if (fields.isEmpty()) {
            return OwnerAnalyticsResponse.builder()
                    .totalBookings(0)
                    .confirmedBookings(0)
                    .cancelledBookings(0)
                    .totalRevenue(BigDecimal.ZERO)
                    .fieldStats(List.of())
                    .build();
        }

        List<Long> fieldIds = fields.stream().map(Field::getId).collect(Collectors.toList());

        // Загружаем все бронирования по полям владельца (без пагинации — для аналитики)
        List<Booking> allBookings = bookingRepository.findByFieldIdIn(fieldIds,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        long totalBookings     = allBookings.size();
        long confirmedBookings = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
        long cancelledBookings = allBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        BigDecimal totalRevenue = allBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(Booking::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Статистика по каждому полю
        List<OwnerAnalyticsResponse.FieldAnalytics> fieldStats = fields.stream().map(field -> {
            List<Booking> fieldBookings = allBookings.stream()
                    .filter(b -> b.getField().getId().equals(field.getId()))
                    .collect(Collectors.toList());

            long fTotal     = fieldBookings.size();
            long fConfirmed = fieldBookings.stream().filter(b -> b.getStatus() == BookingStatus.CONFIRMED).count();
            BigDecimal fRevenue = fieldBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .map(Booking::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return OwnerAnalyticsResponse.FieldAnalytics.builder()
                    .fieldId(field.getId())
                    .fieldName(field.getName())
                    .totalBookings(fTotal)
                    .confirmedBookings(fConfirmed)
                    .revenue(fRevenue)
                    .build();
        }).collect(Collectors.toList());

        return OwnerAnalyticsResponse.builder()
                .totalBookings(totalBookings)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .totalRevenue(totalRevenue)
                .fieldStats(fieldStats)
                .build();
    }
}
