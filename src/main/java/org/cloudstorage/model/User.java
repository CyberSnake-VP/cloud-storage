package org.cloudstorage.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String password;

  // -------------------- РЕАЛИЗАЦИЯ USER DETAILS ----------------------
    /**
     * ВОЗВРАЩАЕТ РОЛИ ПОЛЬЗОВАТЕЛЯ.
     * SimpleGrantedAuthority — простая реализация GrantedAuthority.
     * Префикс "ROLE_" — соглашение Spring Security.
     * Роль "ROLE_USER" будет у всех пользователей.
     * Если бы у нас были админы, мы могли бы вернуть:
     * List.of(new SimpleGrantedAuthority("ROLE_USER"),
     *         new SimpleGrantedAuthority("ROLE_ADMIN"))
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * УЧЁТНАЯ ЗАПИСЬ НЕ ПРОСРОЧЕНА?
     * true — активна.
     * Можно использовать для временных аккаунтов.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * УЧЁТНАЯ ЗАПИСЬ НЕ ЗАБЛОКИРОВАНА?
     * true — не заблокирована.
     * Можно использовать для блокировки нарушителей.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * ПАРОЛЬ НЕ ПРОСРОЧЕН?
     * true — не просрочен.
     * Можно использовать для принудительной смены пароля.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


    /**
     * УЧЁТНАЯ ЗАПИСЬ АКТИВНА?
     * true — активна.
     * Можно использовать для soft-delete пользователей.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
