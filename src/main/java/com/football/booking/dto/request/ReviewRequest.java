package com.football.booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "ID площадки обязателен")
    private Long fieldId;

    @NotNull(message = "Оценка обязательна")
    @Min(value = 1, message = "Минимальная оценка: 1")
    @Max(value = 5, message = "Максимальная оценка: 5")
    private Integer rating;

    @Size(max = 1000, message = "Комментарий не должен превышать 1000 символов")
    private String comment;
}
