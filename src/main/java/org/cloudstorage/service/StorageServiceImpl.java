package org.cloudstorage.service;

import io.minio.*;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * СЕРВИС ДЛЯ РАБОТЫ С ФАЙЛОВЫМ ХРАНИЛИЩЕМ MINIO.
 * Инкапсулирует всю работу с MinIO SDK:
 * - Создание bucket при запуске
 * - Загрузка файлов
 * - Скачивание файлов
 * - Удаление файлов и папок
 * - Создание папок
 * - Поиск файлов
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageServiceImpl implements StorageService {
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * СОЗДАНИЕ КОРЗИНЫ (BUCKET) ПРИ ЗАПУСКЕ.
     * Для каждого пользователя создаём папку внутри bucket'а:
     * user-files/user-1-files/
     * user-files/user-2-files/
     * Этот метод вызывается при старте приложения (позже добавим @PostConstruct).
     */
    @Override
    public void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket {} created successfully", bucketName);
            } else {
                log.info("Bucket {} already exists", bucketName);
            }
        } catch (Exception e) {
            log.error("Error creating bucket: {}", e.getMessage());
            throw new RuntimeException("Could not create bucket: " + e.getMessage());
        }
    }

    /**
     * ЗАГРУЗКА ФАЙЛА.
     * @param userId   ID пользователя
     * @param filePath полный путь к файлу внутри bucket
     * @param file     загружаемый файл (MultipartFile)
     */
    @Override
    public void uploadFile(Long userId, String filePath, MultipartFile file) {
        try {
            // Полный путь с учетом папки пользователя
            String fullPath = getUserPrefix(userId) + filePath;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("File {} uploaded successfully", fullPath);
        } catch (Exception e) {
            log.error("Error uploading file: {}", e.getMessage());
            throw new RuntimeException("Could not upload file: " + e.getMessage());
        }
    }

    @Override
    public byte[] downloadFile(Long userId, String filePath) {
        try {
            String fullPath = getUserPrefix(userId) + filePath;

            // Получаем входящий поток(InputStream)
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build()
            );

            // Переливаем входящий поток в массив байтов в память, а не в файл или в сеть
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            response.transferTo(outputStream);

            // Превращаем в массив байтов
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage());
            throw new RuntimeException("Could not download file: " + e.getMessage());
        }
    }


    /**
     * УДАЛЕНИЕ ФАЙЛА ИЛИ ПАПКИ.
     * В S3 папки — это виртуальное понятие. На самом деле
     * "папка" — это просто префикс в имени файла. (user-1-files/file.txt)
     * На самом деле это файл с таким префиксом. Пустая папка-файл(user-1-files/.)
     * Чтобы удалить "папку", нужно удалить все файлы с этим префиксом.
     */
    @Override
    public void deleteObject(Long userId, String path) {
        try {
            String fullPath = getUserPrefix(userId) + path;

            // Проверяем, это файл или папка
            if (path.endsWith("/")) {
                // Это папка - удаляем все файлы
                deleteFolder(userId, path);
            } else {
                // Это файл - удаляем сам объект
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Error deleting object: {}", e.getMessage());
            throw new RuntimeException("Could not delete object: " + e.getMessage());
        }
    }

    /**
     * СОЗДАНИЕ ПУСТОЙ ПАПКИ.
     * В S3 папки создаются путём создания пустого объекта
     * с именем, заканчивающимся на "/".
     *
     * @param userId     ID пользователя
     * @param folderPath путь к папке
     */
    @Override
    public void createFolder(Long userId, String folderPath) {
        try {
            // Убеждаемся что путь заканчивается на "/"
            String path = folderPath.endsWith("/") ? folderPath : folderPath + "/";
            String fullPath = getUserPrefix(userId) + path;

            // Создаем пустой объект - это и есть "папка" в S3
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            // раз объект пустой и читать нечего, создадим пустой поток
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .contentType("application/x-directory")
                            .build()
            );
            log.info("Folder {} created successfully", fullPath);
        } catch (Exception e) {
            log.error("Error creating folder: {}", e.getMessage());
            throw new RuntimeException("Could not create folder: " + e.getMessage());
        }
    }

    /**
     * ПОЛУЧЕНИЕ СОДЕРЖИМОГО ПАПКИ.
     *
     * @param userId     ID пользователя
     * @param folderPath путь к папке (например, "docs/")
     * @return список имён файлов и папок внутри
     */
    @Override
    public List<String> listFolder(Long userId, String folderPath) {
        List<String> items = new ArrayList<>();
        try {
            String fullPath = getUserPrefix(userId) + folderPath;

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(false) // Только текущий уровень docs/.. (сюда не идем/..)
                            .build()
            );

            for (Result<Item> result : results) {
                Item item = result.get();
                // Убираем префикс пользователя из имени
                String name = item.objectName().replace(getUserPrefix(userId), "");
                if (!name.isEmpty() && !name.equals(folderPath)) {
                    items.add(name);
                }
            }

        } catch (Exception e) {
            log.error("Error listing folder: {}", e.getMessage());
            throw new RuntimeException("Could not list folder: " + e.getMessage());
        }

        return items;
    }

    /**
     * СОЗДАНИЕ BUCKET ПРИ ЗАПУСКЕ ПРИЛОЖЕНИЯ.
     *
     * @PostConstruct — метод вызывается автоматически после создания бина.
     * Это гарантирует, что bucket существует до того, как приложение
     * начнёт принимать запросы.
     */
    @PostConstruct
    private void init() {
        ensureBucketExists();
        log.info("StorageService initialized, bucket '{}' is ready", bucketName);
    }

    /// Проверка на существование ресурса
    @Override
    public boolean exists(Long userId, String path) {
        try {
            String fullPath = getUserPrefix(userId) + path;

            if (fullPath.endsWith("/")) {
                // Папка, проверяем есть ли объект с таким префиксом
                Iterable<Result<Item>> results = minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(fullPath)
                                .maxKeys(1) // Находим только первый объект
                                .build()
                );
                return results.iterator().hasNext();
            } else {
                // Файл проверяем через statObject
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fullPath)
                                .build()
                );
                return true;
            }
        } catch (Exception e) {
            // statObject кидает исключение, если файл не найден
            return false;
        }
    }

    @Override
    public List<String> listFolderRecursive(Long userId, String path) {
        List<String> items = new ArrayList<>();
        try {
            String fullPath = getUserPrefix(userId) + path;
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : results) {
                Item item = result.get();
                String name = item.objectName().replace(getUserPrefix(userId), "");
                if (!name.isEmpty()) {
                    items.add(name);
                }
            }

        } catch (Exception e) {
            log.error("Error listing folder recursive: {}", e.getMessage());
            throw new RuntimeException("Could not list folder recursive: " + e.getMessage());
        }
        return items;
    }

    @Override
    public void uploadFile(Long userId, String filePath, byte[] data) {
        try {
            String fullPath = getUserPrefix(userId) + filePath;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error uploading file from byte array: {}", e.getMessage());
            throw new RuntimeException("Could not upload file: " + e.getMessage());
        }
    }

    @Override
    public List<String> listAllFiles(Long userId) {
        List<String> items = new ArrayList<>();
        try {
            String prefix = getUserPrefix(userId);
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : results) {
                Item item = result.get();
                String name = item.objectName().replace(getUserPrefix(userId), "");
                if (!name.isEmpty()) {
                    items.add(name);
                }
            }

        } catch (Exception e) {
            log.error("Error listing all files from storage: {}", e.getMessage());
            throw new RuntimeException("Could not list all files from storage" + e.getMessage());
        }

        return items;
    }


    /// Получаем размер файла в байтах или Null если файл не найден
    @Override
    public Long getFileSize(Long userId, String filePath) {
        try {
            String fullPath = getUserPrefix(userId) + filePath;

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fullPath)
                            .build()
            );

            return stat.size();
        } catch (Exception e) {
            log.error("Error getting file size: {}", e.getMessage());
            throw new RuntimeException("Could not get file size: " + e.getMessage());
        }
    }

    // --------------------- Утилитарные(вспомогательные) методы -------------------------

    /**
     * Возвращает префикс для файлов пользователя.
     * Пример: user-1-files/
     */
    private String getUserPrefix(Long userId) {
        return "user-" + userId + "-files/";
    }

    private void deleteFolder(Long userId, String folderPath) {
        try {
            String fullPath = getUserPrefix(userId) + folderPath;

            // Получаем список всех файлов в папке (рекурсивно)
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(fullPath)
                            .recursive(true) // все вложенные файлы внутри
                            .build()
            );

            // Удаляем все файлы полученные в списке
            for (Result<Item> result : results) {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(item.objectName())
                                .build()
                );
            }
            log.info("Folder {} deleted successfully", fullPath);
        } catch (Exception e) {
            log.error("Error listing folder: {}", e.getMessage());
            throw new RuntimeException("Could not list folder: " + e.getMessage());
        }

    }
}
