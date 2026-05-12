package com.football.booking.service;

import com.football.booking.entity.*;
import com.football.booking.exception.AccessDeniedException;
import com.football.booking.exception.ResourceNotFoundException;
import com.football.booking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushService;

    // ── Получить или создать комнату чата для бронирования ─────────

    @Transactional
    public ChatRoom getOrCreateRoom(Long bookingId, Authentication auth) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Бронирование не найдено"));

        checkAccess(booking, auth);

        return chatRoomRepository.findByBookingId(bookingId)
                .orElseGet(() -> chatRoomRepository.save(
                        ChatRoom.builder().booking(booking).build()));
    }

    // ── Получить сообщения (all or since) ─────────────────────────

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(Long bookingId, LocalDateTime since, Authentication auth) {
        ChatRoom room = chatRoomRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат не найден"));

        checkAccess(room.getBooking(), auth);

        if (since != null) {
            return chatMessageRepository.findByRoomIdAndSentAtAfterOrderBySentAtAsc(room.getId(), since);
        }
        return chatMessageRepository.findByRoomIdOrderBySentAtAsc(room.getId());
    }

    // ── Отправить сообщение ────────────────────────────────────────

    @Transactional
    public ChatMessage sendMessage(Long bookingId, String content, Authentication auth) {
        ChatRoom room = getOrCreateRoom(bookingId, auth);
        Booking booking = room.getBooking();

        ChatMessage msg = ChatMessage.builder()
                .room(room)
                .senderUsername(auth.getName())
                .content(content.trim())
                .build();

        ChatMessage saved = chatMessageRepository.save(msg);

        // Push уведомление получателю
        String sender   = auth.getName();
        String receiver = sender.equals(booking.getUser().getUsername())
                ? booking.getField().getOwner().getUsername()
                : booking.getUser().getUsername();

        pushService.notify(receiver,
                "Новое сообщение от " + sender,
                content.length() > 80 ? content.substring(0, 77) + "…" : content,
                Map.of("type", "CHAT", "bookingId", String.valueOf(bookingId)));

        return saved;
    }

    // ── Пометить прочитанными ──────────────────────────────────────

    @Transactional
    public void markRead(Long bookingId, Authentication auth) {
        chatRoomRepository.findByBookingId(bookingId).ifPresent(room -> {
            List<ChatMessage> unread = chatMessageRepository
                    .findByRoomIdOrderBySentAtAsc(room.getId())
                    .stream()
                    .filter(m -> !m.getSenderUsername().equals(auth.getName()) && Boolean.FALSE.equals(m.getIsRead()))
                    .toList();
            unread.forEach(m -> m.setIsRead(true));
            chatMessageRepository.saveAll(unread);
        });
    }

    // ── Непрочитанных у пользователя ──────────────────────────────

    @Transactional(readOnly = true)
    public long countUnread(Long bookingId, Authentication auth) {
        return chatRoomRepository.findByBookingId(bookingId)
                .map(room -> chatMessageRepository
                        .countByRoomIdAndIsReadFalseAndSenderUsernameNot(room.getId(), auth.getName()))
                .orElse(0L);
    }

    // ── Проверка доступа ──────────────────────────────────────────

    private void checkAccess(Booking booking, Authentication auth) {
        boolean isAdmin = auth.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
        boolean isParticipant =
                booking.getUser().getUsername().equals(auth.getName())
                || booking.getField().getOwner().getUsername().equals(auth.getName());
        if (!isParticipant && !isAdmin) {
            throw new AccessDeniedException("Нет доступа к этому чату");
        }
    }
}
