package org.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudstorage.dto.ResourceResponse;
import org.cloudstorage.exception.NotFoundException;
import org.cloudstorage.exception.ResourceAlreadyExists;
import org.cloudstorage.service.minio.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final StorageService storageService;

    @Override
    public ResourceResponse getResource(Long userId, String path) {
        // Валидация пути
        validatePath(path);

        // проверяем существование ресурса
        validateExists(userId, path);

        // Определяем тип ресурса и имя. Нужно понять папка это или файл
        boolean isDirectory = path.endsWith("/");
        // Получаем имя - название папки или файла без /
        String name = extractName(path);
        // Получаем тип
        String type = isDirectory ? FileType.DIRECTORY.name() : FileType.FILE.name();
        // Будем записывать размер
        Long size = null;

        // Если у нас путь указывает на файл получаем размер файла или null если не найден
        if (!isDirectory) {
            size = storageService.getFileSize(userId, path);
        }

        return new ResourceResponse(getParentPath(path), name, size, type);
    }

    @Override
    public void deleteResource(Long userId, String path) {
        validatePath(path);
        if (!storageService.exists(userId, path)) {
            throw new NotFoundException("resource not found");
        }
        storageService.deleteObject(userId, path);
    }

    @Override
    public byte[] downloadResource(Long userId, String path) {
        validatePath(path);
        validateExists(userId, path);
        if (path.endsWith("/")) {
            log.info("Downloading folder: {}", path);
            return downloadFolderAsZip(userId, path);
        } else {
            log.info("Downloading file: {}", path);
            return storageService.downloadFile(userId, path);
        }
    }

    @Override
    public ResourceResponse moveResource(Long userId, String from, String to) {
        validatePath(from); // проверка путей
        validatePath(to);
        validateExists(userId, from); // проверка на существование

        // проверяем, что новый путь свободен
        if (storageService.exists(userId, to)) {
            throw new ResourceAlreadyExists("resource already exists for path: " + to);
        }

        // Проверяем папка ли у нас в from
        boolean isDirectory = from.endsWith("/");
        boolean toDirectory = to.endsWith("/");
        if (isDirectory && !toDirectory) {
            throw new IllegalArgumentException("Cannot move directory to a file. Target path must end with '/'");
        }

        if (isDirectory) {
            // У нас папка, значит создадим целевую папку.
            // Создадим новую пустую папку заглушку
            storageService.createFolder(userId, to);

            // Получаем все содержимое папки
            List<String> allItems = storageService.listFolderRecursive(userId, from);

            for (String itemPath : allItems) {
                // Отбрасываем старый путь слева, оставляем после /
                String relativePath = itemPath.substring(from.length());

                // соответственно формируем новый путь
                // doc/report.pdf
                String newItemPath = to + relativePath;

                // может быть itemPath = documents/invoices/,
                // тогда останется invoice/ пустая вложенная папка
                if (itemPath.endsWith("/")) {
                    // отбрасываем левую часть,
                    // потом к новой левой части соединяем вложенную часть и создаем папку если в конце /
                    storageService.createFolder(userId, newItemPath);
                } else {
                    // Значит у нас файл, тогда нужно получить его содержимое и перезолить в новое место
                    byte[] data = storageService.downloadFile(userId, itemPath);
                    storageService.uploadFile(userId, newItemPath, data);
                }
            }
            // удаляем исходную папку со всем ее содержимым
            storageService.deleteObject(userId, from);
        } else {
            // перемещение, переименование файла: скачать - загрузить в новое место - удалить старое
            byte[] data = storageService.downloadFile(userId, from);
            storageService.uploadFile(userId, to, data);
            storageService.deleteObject(userId, from);
        }

        // Формируем DTO получаем размер, если у нас папка то пусто(null)
        Long size = isDirectory ? null : storageService.getFileSize(userId, to);
        return new ResourceResponse(
                getParentPath(to),
                extractName(to),
                size,
                isDirectory ? FileType.DIRECTORY.name() : FileType.FILE.name()
        );
    }

    @Override
    public List<ResourceResponse> searchResources(Long userId, String query) {
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("query parameter cannot be null or empty");
        }
        // Получаем все ресурсы пользователя
        List<String> allItems = storageService.listAllFiles(userId);

        // Делаем поиск в нижнем регистре, через фильтр прогоняем все пути
        String queryLower = query.toLowerCase();
        List<String> matched = allItems.stream()
                .filter(path -> {
                    String name = extractName(path);
                    return name.toLowerCase().contains(queryLower);
                })
                .toList();

        // Проверяем папка или файл, формируем dto, возвращаем список
        return getResourceResponseList(userId, matched);
    }

    @Override
    public List<ResourceResponse> uploadResource(Long userId, String path, MultipartFile file) {
        validatePath(path);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("file is null or empty");
        }

        // path/ + text.txt = path/text.txt, если путь указан, значит есть к файлу поддиректории.
        // Объединяем базовую папку + имя файла (с подпапками)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        String fullPath = path + originalFilename;

        // проверяем существует ли уже такой ресурс
        if (storageService.exists(userId, fullPath)) {
            throw new ResourceAlreadyExists("resource already exists, path: " + fullPath);
        }

        // Записываем файл(сохраняем)
        storageService.uploadFile(userId, fullPath, file);

        // Формируем DTO
        ResourceResponse response = new ResourceResponse(
                path,                           // "storage_folder/"
                extractName(originalFilename),  // "test.txt"
                file.getSize(),
                FileType.FILE.name());

        return List.of(response);
    }

    @Override
    public List<ResourceResponse> getDirectory(Long userId, String path) {

        if(path.isEmpty()) {
            path = "user-" + userId + "-files/";
        }
        validateExists(userId, path);
        List<String> allItems = storageService.listFolder(userId, path);
        return getResourceResponseList(userId, allItems);
    }

    @Override
    public ResourceResponse createDirectory(Long userId, String path) {
        validatePath(path);

        if (!path.endsWith("/")) {
            throw new IllegalArgumentException("Cannot create directory for a file. Target path must end with '/'");
        }

        // Проверяем существование родительской папки
        String parentPath = getParentPath(path);
        if (!parentPath.isEmpty()) {
            validateExists(userId, parentPath);
        }

        // Проверка существование создаваемой папки
        if (storageService.exists(userId, path)) {
            throw new ResourceAlreadyExists("resource already exists, path: " + path);
        }

        storageService.createFolder(userId, path);
        return new ResourceResponse(
                getParentPath(path),
                extractName(path),
                null,
                FileType.DIRECTORY.name()
        );
    }

    /**
     * Извлекает имя файла/папки из полного пути.
     * Примеры:
     * "folder/file.txt" → "file.txt"
     * "folder/subfolder/" → "subfolder"
     * "file.txt" → "file.txt"
     */
    @Override
    public String extractName(String path) {
        // Убираем завершающий /
        String cleanPath = path.endsWith("/") ?
                path.substring(0, path.length() - 1) : path;

        // Находим последний слеш .../name, нужно отрезать все до name
        int lastSlash = cleanPath.lastIndexOf('/'); // индекс слеша с конца
        if (lastSlash >= 0) {
            return cleanPath.substring(lastSlash + 1);
        }
        return cleanPath;
    }

    /**
     * Извлекает родительский путь.
     * Примеры:
     * "folder/file.txt" → "folder/"
     * "folder/subfolder/" → "folder/"
     * "folder/" -> ""
     * "file.txt" → ""
     */
    private String getParentPath(String path) {
        // если у нас папка
        if (path.endsWith("/")) {
            // убираем последний слеш folder/subfolder
            String cleanPath = path.substring(0, path.length() - 1);
            // ищем индекс слеша subfolder или folder/subfolder
            int slashIdx = cleanPath.lastIndexOf('/');
            if (slashIdx >= 0) {
                // значит folder/subfolder - срезаем и возвращаем folder/
                return cleanPath.substring(0, slashIdx + 1);
            }
            // значит корневая папка
            return "";
        } else {
            int slashIdx = path.lastIndexOf('/');
            if (slashIdx >= 0) {
                return path.substring(0, slashIdx + 1);
            } else {
                return "";
            }
        }
    }

    private void validatePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path is null or empty");
        }
    }

    private void validateExists(Long userId, String path) {
        if (!storageService.exists(userId, path)) {
            throw new NotFoundException("resource not found");
        }
    }

    private byte[] downloadFolderAsZip(Long userId, String folderPath) {
        try {
            // Получаем все файлы в папке (рекурсивно) со всеми вложениями. Список полных путей к файлам
            List<String> files = storageService.listFolderRecursive(userId, folderPath);

            log.info("Found {} files", files.size());

            // Создаем поток массива байтов, удобно для возврата byte[] одним методом toByteArray()
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Создаем поток для zip оборачивания
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (String filePath : files) {
                    byte[] fileData = storageService.downloadFile(userId, filePath);

                    // Сохраняем структуру внутри ZIP
                    // срезаем весь путь, оставляем только name файла (folder/folder/) text.txt
                    String entryName = filePath.substring(folderPath.length());

                    // Создаем zip с нашим именем файла
                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);  // кладем архив entry
                    zos.write(fileData);    // после пишем архивные данные внутрь нашего baos
                    zos.closeEntry();      // закрываем архив
                }
            }

            return baos.toByteArray();

        } catch (Exception e) {
            log.warn("Error creating ZIP: {}", e.getMessage());
            throw new RuntimeException("Could not create ZIP archive", e);
        }
    }

    private List<ResourceResponse> getResourceResponseList(Long userId, List<String> allItems) {
        return allItems.stream()
                .map(pathItem -> {
                    boolean isDirectory = pathItem.endsWith("/");
                    Long size = isDirectory ? null : storageService.getFileSize(userId, pathItem);
                    return new ResourceResponse(
                            getParentPath(pathItem),
                            extractName(pathItem),
                            size,
                            isDirectory ? FileType.DIRECTORY.name() : FileType.FILE.name()
                    );
                })
                .toList();
    }
}
