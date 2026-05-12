package com.football.booking.controller;

import com.football.booking.entity.User;
import com.football.booking.enums.Role;
import com.football.booking.enums.VerificationStatus;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.BookingRepository;
import com.football.booking.repository.UserRepository;
import com.football.booking.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/verification")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Верификация владельцев", description = "Управление статусами верификации")
@SecurityRequirement(name = "bearerAuth")
public class OwnerVerificationController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final PushNotificationService pushService;

    /** Список всех владельцев с их статусами верификации */
    @GetMapping("/owners")
    @Operation(summary = "Все владельцы и статус верификации")
    public ResponseEntity<List<OwnerVerificationDto>> listOwners() {
        List<User> owners = userRepository.findByRole(Role.OWNER);
        return ResponseEntity.ok(owners.stream().map(OwnerVerificationDto::from).toList());
    }

    /** Вручную одобрить владельца */
    @PutMapping("/owners/{id}/approve")
    @Operation(summary = "Одобрить владельца (ADMIN_APPROVED)")
    public ResponseEntity<OwnerVerificationDto> approve(@PathVariable Long id) {
        User owner = getOwner(id);
        owner.setVerificationStatus(VerificationStatus.ADMIN_APPROVED);
        userRepository.save(owner);

        pushService.notify(owner.getUsername(),
                "Аккаунт верифицирован ✓",
                "Ваш аккаунт владельца одобрен администратором. Теперь ваши площадки отображаются с badge!",
                Map.of("type", "OWNER_APPROVED"));

        return ResponseEntity.ok(OwnerVerificationDto.from(owner));
    }

    /** Отозвать верификацию */
    @PutMapping("/owners/{id}/revoke")
    @Operation(summary = "Отозвать верификацию")
    public ResponseEntity<OwnerVerificationDto> revoke(@PathVariable Long id) {
        User owner = getOwner(id);
        owner.setVerificationStatus(VerificationStatus.UNVERIFIED);
        userRepository.save(owner);

        pushService.notify(owner.getUsername(),
                "Статус верификации изменён",
                "Ваш статус верификации был отозван. Свяжитесь с поддержкой.",
                Map.of("type", "OWNER_REVOKED"));

        return ResponseEntity.ok(OwnerVerificationDto.from(owner));
    }

    private User getOwner(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден с ID: " + id));
        if (user.getRole() != Role.OWNER && user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Пользователь не является владельцем");
        }
        return user;
    }

    // ── DTO ────────────────────────────────────────────────────────

    public record OwnerVerificationDto(
            Long id,
            String username,
            String email,
            VerificationStatus verificationStatus,
            boolean phoneVerified,
            String phone
    ) {
        static OwnerVerificationDto from(User u) {
            return new OwnerVerificationDto(
                    u.getId(),
                    u.getUsername(),
                    u.getEmail(),
                    u.getVerificationStatus(),
                    Boolean.TRUE.equals(u.getPhoneVerified()),
                    u.getPhone()
            );
        }
    }
}
