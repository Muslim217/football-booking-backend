package com.football.booking.service;

import com.football.booking.entity.DeviceToken;
import com.football.booking.entity.User;
import com.football.booking.repository.DeviceTokenRepository;
import com.football.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Отправка push-уведомлений через Firebase Cloud Messaging (Legacy HTTP API).
 *
 * Настройка:
 *   app.fcm.server-key=<ваш FCM server key из Firebase Console>
 *   app.fcm.enabled=true
 *
 * Если ключ не задан — уведомления тихо пропускаются (режим разработки).
 * Миграция на FCM v1 (OAuth2): заменить sendToToken() на FCM v1 REST endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${app.fcm.server-key:}")
    private String fcmServerKey;

    @Value("${app.fcm.enabled:false}")
    private boolean fcmEnabled;

    // ── Public API ──────────────────────────────────────────────────

    /** Уведомить пользователя по username */
    public void notify(String username, String title, String body) {
        notify(username, title, body, Map.of());
    }

    public void notify(String username, String title, String body, Map<String, String> data) {
        userRepository.findByUsername(username).ifPresent(user ->
                sendToUser(user, title, body, data));
    }

    public void notifyById(Long userId, String title, String body, Map<String, String> data) {
        userRepository.findById(userId).ifPresent(user ->
                sendToUser(user, title, body, data));
    }

    // ── Private helpers ─────────────────────────────────────────────

    private void sendToUser(User user, String title, String body, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(user.getId());
        if (tokens.isEmpty()) {
            log.debug("No device tokens for user {}", user.getUsername());
            return;
        }
        tokens.forEach(dt -> sendToToken(dt.getToken(), title, body, data));
    }

    private void sendToToken(String token, String title, String body, Map<String, String> data) {
        if (!fcmEnabled || fcmServerKey.isBlank()) {
            log.info("[PUSH-DEV] → {} | {} | {}", token.substring(0, Math.min(16, token.length())), title, body);
            return;
        }

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("title", title);
            notification.put("body", body);
            notification.put("sound", "default");

            Map<String, Object> payload = new HashMap<>();
            payload.put("to", token);
            payload.put("notification", notification);
            if (!data.isEmpty()) payload.put("data", data);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "key=" + fcmServerKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(FCM_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Push sent to token ...{}", token.substring(Math.max(0, token.length() - 8)));
            } else {
                log.warn("FCM returned {}: {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to send push to token: {}", e.getMessage());
        }
    }
}
