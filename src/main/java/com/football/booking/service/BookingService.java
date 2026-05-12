package com.football.booking.service;

import com.football.booking.dto.request.BookingRequest;
import com.football.booking.dto.response.BookingResponse;
import com.football.booking.entity.Booking;
import com.football.booking.entity.Field;
import com.football.booking.entity.User;
import com.football.booking.enums.BookingStatus;
import com.football.booking.exception.AccessDeniedException;
import com.football.booking.exception.BookingConflictException;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.BookingRepository;
import com.football.booking.repository.FieldRepository;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FieldRepository fieldRepository;
    private final UserRepository userRepository;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, Authentication authentication) {
        User user = getAuthenticatedUser(authentication);

        Field field = fieldRepository.findById(request.getFieldId())
                .orElseThrow(() -> new ResourceNotFoundException("Площадка не найдена с ID: " + request.getFieldId()));

        if (!field.getIsActive()) {
            throw new IllegalArgumentException("Площадка неактивна и недоступна для бронирования");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("Время окончания должно быть после времени начала");
        }

        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                request.getFieldId(), request.getStartTime(), request.getEndTime());

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException(
                    "Площадка уже забронирована на указанное время");
        }

        long minutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal totalPrice = field.getPricePerHour().multiply(hours).setScale(2, RoundingMode.HALF_UP);

        Booking booking = Booking.builder()
                .user(user)
                .field(field)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalPrice(totalPrice)
                .status(BookingStatus.CONFIRMED)
                .build();

        return mapToResponse(bookingRepository.save(booking));
    }

    public Page<BookingResponse> getMyBookings(Authentication authentication, Pageable pageable) {
        User user = getAuthenticatedUser(authentication);
        return bookingRepository.findByUserId(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public BookingResponse getBookingById(Long id, Authentication authentication) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено с ID: " + id));

        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!booking.getUser().getUsername().equals(authentication.getName()) && !isAdmin) {
            throw new AccessDeniedException("У вас нет доступа к этому бронированию");
        }

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id, Authentication authentication) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено с ID: " + id));

        boolean isAdmin = authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (!booking.getUser().getUsername().equals(authentication.getName()) && !isAdmin) {
            throw new AccessDeniedException("У вас нет прав для отмены этого бронирования");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalArgumentException("Бронирование уже отменено");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return mapToResponse(bookingRepository.save(booking));
    }

    public Page<BookingResponse> getBookingsByField(Long fieldId, Pageable pageable) {
        if (!fieldRepository.existsById(fieldId)) {
            throw new ResourceNotFoundException("Площадка не найдена с ID: " + fieldId);
        }
        return bookingRepository.findByFieldId(fieldId, pageable)
                .map(this::mapToResponse);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .username(booking.getUser().getUsername())
                .fieldId(booking.getField().getId())
                .fieldName(booking.getField().getName())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
