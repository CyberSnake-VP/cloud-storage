package org.cloudstorage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO для ответа с информацией о файле или папке.
 * @JsonInclude(JsonInclude.Include.NON_NULL) — поля со значением null
 * не попадут в JSON ответ. Это удобно: size отображается только для файлов.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceResponse(
        String path,      // Пример: "folder1/folder2/"
        String name,      // Пример: "file.txt"
        Long size,        // Только для файлов, в байтах
        String type       // FILE или DIRECTORY
) {
}
