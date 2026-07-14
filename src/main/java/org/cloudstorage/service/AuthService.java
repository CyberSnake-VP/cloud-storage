package org.cloudstorage.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;

    public Authentication authenticationUser(HttpServletRequest request,
                                             String username,
                                             String password) {
        log.debug("Authenticating user {} with password {}", username, password);

        // 1. Создаём токен
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(username, password);

        // 2. Аутентифицируем
        Authentication authentication = authenticationManager.authenticate(token);

        // 3. Сохраняем в SecurityContext
        SecurityContext securityContext = SecurityContextHolder.getContext();
        securityContext.setAuthentication(authentication);

        // 4. Сохраняем в сессию
        HttpSession session = request.getSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);

        log.debug("User {} authenticated successfully", username);
        return authentication;
    }

}
