package com.football.booking.service;

import com.football.booking.dto.request.ForgotPasswordRequest;
import com.football.booking.dto.request.ResetPasswordRequest;
import com.football.booking.dto.response.MessageResponse;
import com.football.booking.entity.PasswordResetToken;
import com.football.booking.entity.User;
import com.football.booking.repository.PasswordResetTokenRepository;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username:dev@example.com}")
    private String mailFrom;

    @Transactional
    public MessageResponse requestReset(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        // Не раскрываем, зарегистрирован ли email
        if (userOpt.isEmpty()) {
            return new MessageResponse("Если email зарегистрирован, вы получите письмо со ссылкой для сброса пароля");
        }

        User user = userOpt.get();

        // Удаляем старые токены
        tokenRepository.deleteByUserId(user.getId());

        String tokenValue = UUID.randomUUID().toString();
        PasswordResetToken token = PasswordResetToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(1)) // действует 1 час
                .build();
        tokenRepository.save(token);

        String resetLink = baseUrl + "/api/auth/reset-password?token=" + tokenValue;
        sendResetEmail(user.getEmail(), resetLink);

        return new MessageResponse("Если email зарегистрирован, вы получите письмо со ссылкой для сброса пароля");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Недействительный или истёкший токен сброса пароля"));

        if (!token.isValid()) {
            throw new IllegalArgumentException("Токен сброса пароля истёк. Запросите новый.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        tokenRepository.save(token);

        return new MessageResponse("Пароль успешно изменён. Войдите с новым паролем.");
    }

    private void sendResetEmail(String email, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(email);
            message.setSubject("Сброс пароля — Площадка");
            message.setText(
                    "Вы запросили сброс пароля.\n\n" +
                    "Перейдите по ссылке для установки нового пароля:\n" +
                    resetLink + "\n\n" +
                    "Ссылка действительна 1 час.\n\n" +
                    "Если вы не запрашивали сброс пароля — проигнорируйте это письмо."
            );
            mailSender.send(message);
            log.info("Письмо для сброса пароля отправлено на {}", email);
        } catch (Exception e) {
            // В dev режиме SMTP не настроен — просто логируем ссылку
            log.warn("Не удалось отправить письмо на {}. Ссылка для сброса: {}", email, resetLink);
        }
    }
}
