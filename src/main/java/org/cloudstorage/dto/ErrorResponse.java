package org.cloudstorage.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Ответ с сообщением об ошибке")
public record ErrorResponse(

        @Schema(description = "Текст ошибки")
        String message
) {
}
