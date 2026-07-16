package org.cloudstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с информацией о пользователе")
public record UserResponse(

        @Schema(description = "Имя пользователя", example = "john_doe")
        String username
) {
}
