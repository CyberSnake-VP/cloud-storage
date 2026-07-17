package org.cloudstorage.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.cloudstorage.dto.AuthRequest;
import org.cloudstorage.dto.ResourceResponse;
import org.cloudstorage.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc; // Имитация HTTP сервера, отправляем запросы

    @MockitoBean
    private ResourceService resourceService;  // Мокаем сервис — MinIO не нужен!

    @Autowired
    private ObjectMapper objectMapper;

    private MockHttpSession session;

    // Логинимся и создаем общую http сессию для мок тестов
    @BeforeEach
    public void setup() throws Exception {
        session = new MockHttpSession();

        AuthRequest authRequest = new AuthRequest("username", "password");
        mockMvc.perform(post("/api/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest))
                        .session(session))
                .andExpect(status().isCreated());
    }


    @Test
    @DisplayName("GET /api/resource - 200 OK")
    void getResource_Authorized() throws Exception {

        // Настраиваем Мок
        ResourceResponse mockResp = new ResourceResponse("docs/", "test.txt", 100L, "FILE");

        when(resourceService.getResource(anyLong(), eq("docs/test.txt")))
                .thenReturn(mockResp);

        // Мы создали общую мок сессию, указали ее при регистрации, теперь нет проблем с доступом
        mockMvc.perform(get("/api/resource")
                        .param("path", "docs/test.txt")
                        .session(session)) // используем сохраненную сессию
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.size").value(100L));

    }

    @Test
    @DisplayName("GET /api/resource - 401 без авторизации")
    void getResource_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/resource"))
                .andExpect(status().isForbidden());
    }

    // метод deleteResource в сервисе возвращает void, раз сервис мокнут,
    // то метод просто проигнорируется
    @Test
    @DisplayName("DELETE /api/resource - 204 No Content")
    void deleteResource_NoContent() throws Exception {
        mockMvc.perform(delete("/api/resource")
                        .param("path", "docs/test.txt")
                        .session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/resource/search - 400 пустой запрос")
    void search_EmptyQuery_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/resource/search")
                        .param("query", "")
                        .session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/directory - 201 Created")
    void createDirectory_Authorized() throws Exception {
        ResourceResponse mockResp =
                new ResourceResponse("", "newfolder", null, "DIRECTORY");

        when(resourceService.createDirectory(anyLong(), eq("newfolder/")))
                .thenReturn(mockResp);

        mockMvc.perform(post("/api/directory")
                        .param("path", "newfolder/")
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("newfolder"));
    }

}
