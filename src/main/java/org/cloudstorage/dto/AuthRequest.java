package org.cloudstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса на регистрацию и авторизацию.
 * - POST /api/auth/sign-up (регистрация)
 * - POST /api/auth/sign-in (вход)
 */
@Schema(description = "Запрос на регистрацию или авторизацию")
public record AuthRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 an 50 characters")
        @Schema(description = "Имя пользователя", example = "john_doe", minLength = 3, maxLength = 50)
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 5, message = "Password must be least 5 characters")
        @Schema(description = "Пароль", example = "securePassword123", minLength = 5)
        String password
) {
}
