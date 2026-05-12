package com.football.booking.controller;

import com.football.booking.entity.ChatMessage;
import com.football.booking.entity.ChatRoom;
import com.football.booking.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Чат", description = "Чат между клиентом и владельцем площадки")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    /** Получить (или создать) комнату чата для бронирования */
    @GetMapping("/bookings/{bookingId}/room")
    @Operation(summary = "Получить/создать комнату чата")
    public ResponseEntity<Map<String, Long>> getRoom(
            @PathVariable Long bookingId,
            Authentication auth) {
        ChatRoom room = chatService.getOrCreateRoom(bookingId, auth);
        return ResponseEntity.ok(Map.of("roomId", room.getId(), "bookingId", bookingId));
    }

    /** Получить все сообщения (или только с указанного момента для polling) */
    @GetMapping("/bookings/{bookingId}/messages")
    @Operation(summary = "Получить сообщения чата")
    public ResponseEntity<List<ChatMessageDto>> getMessages(
            @PathVariable Long bookingId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            Authentication auth) {
        List<ChatMessage> msgs = chatService.getMessages(bookingId, since, auth);
        chatService.markRead(bookingId, auth);
        return ResponseEntity.ok(msgs.stream().map(ChatMessageDto::from).toList());
    }

    /** Отправить сообщение */
    @PostMapping("/bookings/{bookingId}/messages")
    @Operation(summary = "Отправить сообщение")
    public ResponseEntity<ChatMessageDto> sendMessage(
            @PathVariable Long bookingId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String content = body.getOrDefault("content", "").trim();
        if (content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        ChatMessage msg = chatService.sendMessage(bookingId, content, auth);
        return ResponseEntity.ok(ChatMessageDto.from(msg));
    }

    /** Количество непрочитанных сообщений */
    @GetMapping("/bookings/{bookingId}/unread")
    @Operation(summary = "Кол-во непрочитанных")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @PathVariable Long bookingId,
            Authentication auth) {
        return ResponseEntity.ok(Map.of("count", chatService.countUnread(bookingId, auth)));
    }

    // ── Inner DTO ──────────────────────────────────────────────────

    public record ChatMessageDto(
            Long id,
            String senderUsername,
            String content,
            LocalDateTime sentAt,
            boolean isRead
    ) {
        static ChatMessageDto from(ChatMessage m) {
            return new ChatMessageDto(
                    m.getId(),
                    m.getSenderUsername(),
                    m.getContent(),
                    m.getSentAt(),
                    Boolean.TRUE.equals(m.getIsRead())
            );
        }
    }
}
