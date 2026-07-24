package org.cloudstorage.controller;


import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudstorage.dto.ResourceResponse;
import org.cloudstorage.model.User;
import org.cloudstorage.service.ResourceService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * КОНТРОЛЛЕР ДЛЯ РАБОТЫ С ФАЙЛАМИ И ПАПКАМИ.
 * Все эндпоинты доступны только авторизованным пользователям.
 * Пути: /api/resource и /api/directory
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    /**
     * ЗАГРУЗКА ФАЙЛА.
     * POST /api/resource?path=folder/
     * Тело запроса: multipart/form-data с файлом
     */
    @PostMapping("/resource")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceResponse> uploadResource(@AuthenticationPrincipal User user,
                                                 @RequestParam(defaultValue = "") String path,
                                                 @RequestParam("object") List<MultipartFile> files) {
        log.info("POST /resource started: for userId={}", user.getId());
        List<ResourceResponse> result = resourceService.uploadResource(user.getId(), path, files);
        log.info("POST /resource finished: for userId={}, size={}", user.getId(), result.size());
        return result;
    }

    /**
     * ПОЛУЧЕНИЕ ИНФОРМАЦИИ О РЕСУРСЕ.
     * GET /api/resource?path=folder/file.txt
     *
     * @param user текущий пользователь
     * @param path путь к ресурсу
     * @return информация о файле или папке
     */
    @GetMapping("/resource")
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponse getResource(@AuthenticationPrincipal User user,
                                        @RequestParam String path) {
        log.info("GET /resource started with userId={}, and path={}", user.getId(), path);
        ResourceResponse result = resourceService.getResource(user.getId(), path);
        log.info("GET /resource finished: for userId={}", user.getId());
        return result;
    }

    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteResource(@AuthenticationPrincipal User user,
                               @RequestParam String path) {
        log.info("DELETE /resource started with userId={}", user.getId());
        resourceService.deleteResource(user.getId(), path);
        log.info("DELETE /resource finished: for userId={}", user.getId());
    }

    @GetMapping("/resource/download")
    public ResponseEntity<Resource> downloadResource(@AuthenticationPrincipal User user,
                                                     @RequestParam String path) {
        log.info("GET /resource/dowload started with userId={}", user.getId());
        return path.endsWith("/")?
                downloadFolder(user, path):
                downloadFile(user, path);
    }

    private ResponseEntity<Resource> downloadFile(User user, String path) {
        InputStream inputStream = resourceService.downloadFile(user.getId(), path);

        String name = resourceService.extractName(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + name + "\"")
                .body(new InputStreamResource(inputStream));
    }

    private ResponseEntity<Resource> downloadFolder(User user, String path) {
           return null;
    }

    @PostMapping("/resource/move")
    @ResponseStatus(HttpStatus.OK)
    public ResourceResponse moveResource(@AuthenticationPrincipal User user,
                                         @RequestParam String from,
                                         @RequestParam String to) {
        log.info("POST /resource/move started for userId={}, from={}, to={}", user.getId(), from, to);
        ResourceResponse result = resourceService.moveResource(user.getId(), from, to);
        log.info("POST /resource/move finished: for userId={}", user.getId());
        return result;
    }

    @GetMapping("/resource/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceResponse> searchResource(@AuthenticationPrincipal User user,
                                                 @RequestParam @NotBlank String query) {
        log.info("GET /resource/search started for userId={}, query={}", user.getId(), query);
        List<ResourceResponse> result = resourceService.searchResources(user.getId(), query);
        log.info("GET /resource/search finished: for userId={}, query={}", user.getId(), query);
        return result;
    }

    @GetMapping("/directory")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceResponse> getDirectory(@AuthenticationPrincipal User user,
                                               @RequestParam String path) {
        log.info("GET /directory started for userId={}, path={}", user.getId(), path);
        List<ResourceResponse> result = resourceService.getDirectory(user.getId(), path);
        log.info("GET /directory finished: for userId={}, size={}", user.getId(), result.size());
        return result;
    }

    @PostMapping("/directory")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponse createDirectory(@AuthenticationPrincipal User user,
                                            @RequestParam String path) {
        log.info("POST /directory started for userId={}", user.getId());
        ResourceResponse result = resourceService.createDirectory(user.getId(), path);
        log.info("POST /directory finished: for userId={}", user.getId());
        return result;
    }

}
