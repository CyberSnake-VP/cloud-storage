package org.cloudstorage.config;

import lombok.RequiredArgsConstructor;
import org.cloudstorage.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // можно опустить, но для примера оставим
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;

    /**
     * UserDetailsService — ЗАГРУЖАЕТ пользователя из БД.
     * Когда кто-то пытается залогиниться, Spring Security вызывает
     * loadUserByUsername(username), чтобы найти пользователя, через лямбду работаем с username.
     * <p>
     * ВАЖНО: Наш класс User должен реализовывать UserDetails!
     * <p>
     * По сути я указываю КАК НАЙТИ ПОЛЬЗОВАТЕЛЯ
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with username: " + username));
    }

    // УКАЗЫВАЮ КАК ПРОВЕРИТЬ ПАРОЛЬ
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }


    /**
     * SecurityFilterChain — ОПРЕДЕЛЯЕТ ПРАВИЛА БЕЗОПАСНОСТИ.
     * Здесь мы говорим Spring Security:
     * - Какие URL доступны без аутентификации
     * - Какие URL требуют входа
     * - Как обрабатывать логин и логаут
     * - Нужна ли защита от CSRF
     * - Как управлять сессиями
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)

                // -- СЕССИИ --
                .sessionManagement(session -> session
                        // сессия создается при логине, не раньше
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // Максимум 1 сессия на пользователя
                        .maximumSessions(1)
                        // false = новая сессия вытесняет старую
                        // true = запретить новый логин, если уже есть сессия
                        .maxSessionsPreventsLogin(false)
                )

                // -- ПРАВИЛА ДОСТУПА --
                .authorizeHttpRequests(auth -> auth
                        // РАЗРЕШЕНО ВСЕМ (без аутентификации):
                        .requestMatchers("/api/auth/**").permitAll() // регистрация и логин

                        // SWAGGER
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**"
                        ).permitAll()

                        // ВСЕ ОСТАЛЬНОЕ ТРЕБУЕТ аутентификации
                        .anyRequest().authenticated()
                )

                // -- ОТКЛЮЧАЕМ СТАНДАРТНУЮ ФОРМУ ЛОГИНА --
                // Будем логинить через свой контроллер (POST /api/auth/sign-in
                .formLogin(AbstractHttpConfigurer::disable)

                // -- НАСТРОЙКА ЛОГАУТА --
                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out") // URL выхода
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(204); // 204 No Content
                        })
                        .invalidateHttpSession(true) // Удаляем сессию
                        .deleteCookies("JSESSIONID") // Удаляем куку
                );

        return http.build();
    }
}


