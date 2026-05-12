package com.football.booking.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 100, message = "Имя не может быть длиннее 100 символов")
    private String fullName;

    @Pattern(regexp = "^\\+?[0-9\\s\\-()]{7,20}$", message = "Некорректный формат номера телефона")
    private String phone;
}
