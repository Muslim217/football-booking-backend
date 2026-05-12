package com.football.booking.dto.response;

import com.football.booking.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;
    private String username;
    private Long fieldId;
    private String fieldName;
    private String fieldAddress;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalPrice;
    private BookingStatus status;
    private LocalDateTime createdAt;
}
