package com.football.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long fieldId;
    private String fieldName;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
