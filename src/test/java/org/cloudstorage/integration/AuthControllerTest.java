package org.cloudstorage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudstorage.dto.AuthRequest;
import org.cloudstorage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ИНТЕГРАЦИОННЫЙ ТЕСТ КОНТРОЛЛЕРА АВТОРИЗАЦИИ.
 *
 * @SpringBootTest — поднимает ВЕСЬ Spring контекст
 * @AutoConfigureMockMvc — создаёт MockMvc для эмуляции HTTP
 * @ActiveProfiles("test") — использует H2 вместо PostgreSQL
 * @Transactional — откатывает изменения после каждого теста
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Эмулятор HTTP-запросов

    @Autowired
    private ObjectMapper objectMapper; // Для преобразования объектов в JSON

    @Autowired
    private UserRepository userRepository;  // Для очистки БД

    @BeforeEach
    public void setup() {
        // Очищаем БД перед каждым тестом
        userRepository.deleteAll();
    }


    // РЕГИСТРАЦИЯ (sign-up)
    @Test
    @DisplayName("POST /api/auth/sign-up - успешная регистрация (201)")
    void signUp_Success_ShouldReturn201() throws Exception {
        // Подготовка запроса
        AuthRequest request = new AuthRequest("newuser", "password123");
        String json = objectMapper.writeValueAsString(request);

        //Выполняем запрос и проверяем ответ
        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())  // 201
                .andExpect(jsonPath("$.username").value("newuser")); // тело ответа

    }

    @Test
    @DisplayName("POST /api/auth/sign-up - 409 дубликат")
    void signUp_Duplicate_ShouldReturn409() throws Exception {
        AuthRequest request = new AuthRequest("taken", "password123");
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));

        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    // ВХОД sign-in
    @Test
    @DisplayName("POST /api/auth/sign-in -200 OK")
    void signIn_Success_ShouldReturn200() throws Exception {
        AuthRequest request = new AuthRequest("newuser", "password123");
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
        // выхожу из учетки
        mockMvc.perform(post("/api/auth/sign-out"));
        // логинюсь
        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"));

    }

    @Test
    @DisplayName("POST /api/auth/sign-in - 401 Неверный пароль")
    void signIn_Wrong_Password_ShouldReturn401() throws Exception {
        AuthRequest request = new AuthRequest("newuser", "password123");
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
        mockMvc.perform(post("/api/auth/sign-out"));

        AuthRequest badRequest = new AuthRequest("newuser", "wrong");
        String jsonWrong = objectMapper.writeValueAsString(badRequest);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWrong))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/sign-in - 401 Пользователь не существует")
    void signIn_UserNotFound_ShouldReturn401() throws Exception {
        AuthRequest request = new AuthRequest("newuser", "password123");
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }

    // ВЫХОД sign-out
    @Test
    @DisplayName("POST /api/auth/sign-out - 204 No Content")
    void signOut_Success_ShouldReturn204() throws Exception {
        mockMvc.perform(post("/api/auth/sign-out"))
                .andExpect(status().isNoContent());
    }

}
