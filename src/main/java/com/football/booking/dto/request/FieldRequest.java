package com.football.booking.dto.request;

import com.football.booking.enums.FieldType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FieldRequest {

    @NotBlank(message = "Название поля обязательно")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Адрес обязателен")
    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String city;

    @NotNull(message = "Тип площадки обязателен")
    private FieldType fieldType;

    @NotNull(message = "Цена за час обязательна")
    @DecimalMin(value = "0.0", inclusive = false, message = "Цена должна быть больше нуля")
    private BigDecimal pricePerHour;

    @Size(max = 1000)
    private String description;

    private String photoUrl;
}
