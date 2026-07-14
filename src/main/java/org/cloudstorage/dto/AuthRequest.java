package org.cloudstorage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для запроса на регистрацию и авторизацию.
 * - POST /api/auth/sign-up (регистрация)
 * - POST /api/auth/sign-in (вход)
 */
public record AuthRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 an 50 characters")
        String username,
        @NotBlank(message = "Password is required")
        @Size(min = 4, message = "Password must be least 4 characters")
        String password
) {
}
