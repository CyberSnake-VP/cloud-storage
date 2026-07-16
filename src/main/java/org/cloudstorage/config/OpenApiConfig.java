package org.cloudstorage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cloudStorageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Облачное хранилище файлов API")
                        .description("""
                                                        Многопользовательское файловое облако
                                
                                                        Аналог Google Drive. Пользователи могут загружать,
                                                        хранить, скачивать файлы и обмениваться ими.
                                
                                                        🔐 Авторизация
                                                                - Регистрация нового пользователя
                                                                - Вход в систему
                                                                - Выход из системы
                                
                                                        📁 Работа с файлами
                                                                - Загрузка файлов
                                                                - Скачивание (папки — ZIP)
                                                                - Переименование и перемещение
                                                                - Поиск по имени
                                                                - Удаление
                                
                                                        🛠 Технологии
                                                                - Java 17, Spring Boot 3, Spring Security
                                                                - PostgreSQL, MinIO (S3), Redis
                                                                - Liquibase, Swagger
                                """)
                        .version("1.0")
                        .contact(new Contact()
                                .name("Vadim")
                                .email("pimenov.vdm@gmail.com")));
    }
}
