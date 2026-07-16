package org.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO для ответа с информацией о файле или папке.
 * @JsonInclude(JsonInclude.Include.NON_NULL) — поля со значением null
 * не попадут в JSON ответ. Это удобно: size отображается только для файлов.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Информация о файле или папке")
public record ResourceResponse(
        @Schema(description = "Путь к родительской папке", example = "docs/images/")
        String path,
        @Schema(description = "Имя файла или папки", example = "photo.jpg")
        String name,
        @Schema(description = "Размер файла в байтах (только для файлов)", example = "102400")
        Long size,
        @Schema(description = "Тип ресурса", example = "FILE", allowableValues = {"FILE", "DIRECTORY"})
        String type
) {
}
