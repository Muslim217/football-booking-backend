package com.football.booking.dto.response;

import com.football.booking.enums.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldResponse {

    private Long id;
    private String name;
    private String address;
    private String city;
    private FieldType fieldType;
    private BigDecimal pricePerHour;
    private String description;
    private String photoUrl;
    private Boolean isActive;
    private String ownerUsername;
    private LocalDateTime createdAt;
}
