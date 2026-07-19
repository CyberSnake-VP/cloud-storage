# ☁️ Облачное хранилище файлов (Cloud Storage)

Многопользовательское файловое облако — аналог Google Drive.  
Пользователи могут регистрироваться, загружать, хранить, скачивать файлы и управлять папками.

---

## 🛠 Технологии

| Слой | Технологии |
|------|-----------|
| **Бэкенд** | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Session |
| **База данных** | PostgreSQL 16 |
| **Файловое хранилище** | MinIO (S3-совместимое) |
| **Сессии** | Redis |
| **Миграции** | Liquibase |
| **Фронтенд** | React (собран через Vite), Nginx |
| **Документация API** | Swagger / OpenAPI 3 |
| **Тесты** | JUnit 5, Mockito, Testcontainers |
| **Контейнеризация** | Docker, Docker Compose |

---

## 🚀 Быстрый старт

### Требования

- Docker и Docker Compose
- Порт 80 свободен

### Запуск

```bash
git clone <your-repo-url>
cd cloud-storage
docker compose up -d --build
```

| Сервис | URL |
|--------|-----|
| **Фронтенд** | http://localhost |
| **API (прямой доступ)** | http://localhost:8080 |
| **Swagger** | http://localhost:8080/swagger-ui.html |
| **MinIO Console** | http://localhost:9001 (minioadmin / minioadmin) |

## 📁 REST API

Все эндпоинты доступны под общим префиксом `/api`.

### 🔐 Авторизация

| Метод | URL | Описание |
|-------|-----|----------|
| `POST` | `/api/auth/sign-up` | Регистрация |
| `POST` | `/api/auth/sign-in` | Вход |
| `POST` | `/api/auth/sign-out` | Выход |

### 👤 Пользователи

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/user/me` | Текущий пользователь |

### 📂 Ресурсы

| Метод | URL | Описание |
|-------|-----|----------|
| `GET` | `/api/resource?path=` | Информация о файле/папке |
| `DELETE` | `/api/resource?path=` | Удаление |
| `GET` | `/api/resource/download?path=` | Скачивание (папки — ZIP) |
| `POST` | `/api/resource/move?from=&to=` | Перемещение/переименование |
| `GET` | `/api/resource/search?query=` | Поиск по имени |
| `POST` | `/api/resource?path=` | Загрузка файла |
| `GET` | `/api/directory?path=` | Содержимое папки |
| `POST` | `/api/directory?path=` | Создание папки |

Подробная документация: http://localhost:8080/swagger-ui.html

## 🏗 Архитектура

### Путь запроса

| Шаг | Компонент | Действие |
|-----|-----------|----------|
| 1 | **Пользователь** | Открывает `http://localhost` |
| 2 | **Nginx (:80)** | Отдаёт React (/) или проксирует на бэкенд (/api/*) |
| 3 | **Spring Boot (:8080)** | Обрабатывает запрос |
| 4 | **PostgreSQL** | Хранит пользователей и метаданные |
| 4 | **Redis** | Хранит сессии |
| 4 | **MinIO** | Хранит файлы |

