package org.cloudstorage.testcontainers;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.cloudstorage.service.minio.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/// ЧТО ПРОВЕРЯЕМ? СЛОЙ СЕРВИСА И ПРЯМУЮ РАБОТУ С MINIO, РЕАЛЬНОЕ СОХРАНЕНИЕ ФАЙЛОВ И ПАПОК
///  ЛОГИКУ РАБОТУ RESOURCE SERVICE ПРОВЕРИЛИ UNIT тестами быстрыми.
@SpringBootTest
@Testcontainers
@ActiveProfiles("minio")
public class StorageServiceMinioTest {

    // POSTGRESQL В DOCKER
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("minio_test_db")
            .withUsername("test_user")
            .withPassword("test_password");

    // MINIO В DOCKER
    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:latest")
            .withUserName("minioadmin")
            .withPassword("minioadmin");


    // ДИНАМИЧЕСКАЯ КОНФИГУРАЦИЯ
    // НАСТРАИВАЕМ ПОДКЛЮЧЕНИЕ К СЕРВИСАМ
    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // MinIO
        registry.add("minio.endpoint", minio::getS3URL);
        registry.add("minio.access-key", minio::getUserName);
        registry.add("minio.secret-key", minio::getPassword);
        registry.add("minio.bucket-name", () -> "test-user-files");
    }


    // ТЕСТИРУЕМЫЕ ОБЪЕКТЫ
    @Autowired
    private StorageService storageService;  // будем тестировать сервисный слой

    @Autowired
    private MinioClient minioClient;  // настроим создание бакетов

    // id пользователей (не используем секьюрити) по сути мокаем этот момент
    private static final Long USER_1 = 1L;
    private static final Long USER_2 = 2L;

    // Каждый раз создаем новый бакет перед каждым тестом
    @BeforeEach
    void setUp() throws Exception {
        // Создаем бакет перед каждым тестом
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket("test-user-files").build());
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket("test-user-files").build()
            );
        }
    }

    // ЗАГРУЗКА ФАЙЛОВ
    @Test
    @DisplayName("Загрузка файла - появляется в MinIO в папке пользователя")
    void uploadFile_ShouldStoreInUserFolder() {
        storageService.uploadFile(USER_1, "docs/report.txt", "Report content".getBytes());

        // файл существует в папке пользователя
        assertThat(storageService.exists(USER_1, "docs/report.txt")).isTrue();
    }

    // СКАЧИВАНИЕ
    @Test
    @DisplayName("Скачивание файла - возвращает те же данные")
    void downloadFile_ShouldRerunSameData() {
        byte[] original = "Test data".getBytes();
        storageService.uploadFile(USER_1, "docs/report.txt", original);

        byte[] downloaded = storageService.downloadResource(USER_1, "docs/report.txt");

        assertThat(downloaded).isEqualTo(original);
    }

    @Test
    @DisplayName("Удаление файла - исчезает из MinIO")
    void deleteFile_ShouldRemove() {
        storageService.uploadFile(USER_1, "docs/report.txt", "Report content".getBytes());
        assertThat(storageService.exists(USER_1, "docs/report.txt")).isTrue();

        storageService.deleteObject(USER_1, "docs/report.txt");

        assertThat(storageService.exists(USER_1, "docs/report.txt")).isFalse();
    }

    @Test
    @DisplayName("Изоляция пользователей - USER_2 не видит файлы USER_1")
    void userIsolation() {
        storageService.uploadFile(USER_1, "docs/report.txt", "User1 secret".getBytes());
        storageService.uploadFile(USER_2, "docs/report.txt", "User2 secret".getBytes());

        byte[] userFile1 = storageService.downloadResource(USER_1, "docs/report.txt");
        assertThat(userFile1).isEqualTo("User1 secret".getBytes());
        assertThat(userFile1).isNotEqualTo("User2 secret".getBytes());

        byte[] userFile2 = storageService.downloadResource(USER_2, "docs/report.txt");
        assertThat(userFile2).isEqualTo("User2 secret".getBytes());
        assertThat(userFile2).isNotEqualTo("User1 secret".getBytes());

    }

}
