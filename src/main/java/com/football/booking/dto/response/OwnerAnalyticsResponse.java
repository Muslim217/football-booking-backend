package com.football.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerAnalyticsResponse {

    private long totalBookings;
    private long confirmedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue;
    private List<FieldAnalytics> fieldStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldAnalytics {
        private Long fieldId;
        private String fieldName;
        private long totalBookings;
        private long confirmedBookings;
        private BigDecimal revenue;
    }
}
