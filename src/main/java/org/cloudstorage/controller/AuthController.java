package org.cloudstorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudstorage.dto.AuthRequest;
import org.cloudstorage.dto.ErrorResponse;
import org.cloudstorage.dto.UserResponse;
import org.cloudstorage.model.User;
import org.cloudstorage.service.AuthService;
import org.cloudstorage.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


@Tag(name = "🔐 Авторизация",
        description = "Регистрация, вход и выход пользователей")
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    /**
     * РЕГИСТРАЦИЯ НОВОГО ПОЛЬЗОВАТЕЛЯ.
     * Процесс:
     * 1. Валидация входных данных (аннотации @Valid)
     * 2. Регистрация пользователя в БД
     * 3. Автоматический вход после регистрации
     * 4. Создание сессии
     *
     * @param request     данные пользователя (username, password) - dto
     * @param httpRequest HTTP запрос (для создания сессии)
     * @return 201 Created с данными пользователя
     */
    @Operation(summary = "Регистрация")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "OK"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username занят",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signUp(@Valid @RequestBody AuthRequest request,
                                    HttpServletRequest httpRequest) {

        // Регистрируем пользователя
        User user = userService.register(request.username(), request.password());

        // Автоматически логиним после регистрации, делаем ему вход в систему через наш метод
        authService.authenticationUser(httpRequest, request.username(), request.password());

        log.info("User registered successfully: {}", user.getUsername());

        // возвращаем пользователя после регистрации
        return new UserResponse(user.getUsername());
    }


    /**
     * ВХОД ПОЛЬЗОВАТЕЛЯ.
     * Процесс:
     * 1. Валидация входных данных
     * 2. Аутентификация через AuthenticationManager
     * 3. Сохранение SecurityContext в сессии
     *
     * @param request     данные пользователя (username, password)
     * @param httpRequest HTTP запрос (для создания сессии)
     * @return 200 OK с данными пользователя
     */
    @Operation(summary = "Вход")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "401", description = "Неверный логин или пароль",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/sign-in")
    @ResponseStatus(HttpStatus.OK)
    public UserResponse signIn(@Valid @RequestBody AuthRequest request,
                                    HttpServletRequest httpRequest) {
        // Аутентификация пользователя
        Authentication auth = authService.authenticationUser(httpRequest, request.username(), request.password());

        // получаем аутентифицированного пользователя
        User user = (User) auth.getPrincipal();
        log.info("User logged in successfully: {}", user.getUsername());

        // Возвращаем пользователя(DTO)
        return new UserResponse(user.getUsername());
    }

    /**
     * ВЫХОД ИЗ СИСТЕМЫ.
     * Spring Security автоматически обрабатывает запрос к /api/auth/sign-out.
     * Метод нужен для документирования в Swagger.
     */
    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut() {
        return ResponseEntity.noContent().build();
    }
}
