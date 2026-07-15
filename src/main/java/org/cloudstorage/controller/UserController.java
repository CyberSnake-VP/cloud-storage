package org.cloudstorage.controller;


import org.cloudstorage.dto.UserResponse;
import org.cloudstorage.model.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    /**
     * ТЕКУЩИЙ ПОЛЬЗОВАТЕЛЬ.
     * @AuthenticationPrincipal — Spring Security автоматически
     * подставляет текущего аутентифицированного пользователя.
     * Если пользователь не авторизован — вернётся 401 (настроено в SecurityConfig).
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")  // Автоматическая проверка(если user == null)
    public UserResponse getCurrentUser(@AuthenticationPrincipal User user) {
        return new UserResponse(user.getUsername());
    }
}
