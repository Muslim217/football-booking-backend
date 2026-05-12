package com.football.booking.repository;

import com.football.booking.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderBySentAtAsc(Long roomId);
    List<ChatMessage> findByRoomIdAndSentAtAfterOrderBySentAtAsc(Long roomId, LocalDateTime since);
    long countByRoomIdAndIsReadFalseAndSenderUsernameNot(Long roomId, String username);
}
