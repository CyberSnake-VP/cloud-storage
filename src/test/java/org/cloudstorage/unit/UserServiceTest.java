package org.cloudstorage.unit;

import org.cloudstorage.exception.UserAlreadyExistException;
import org.cloudstorage.model.User;
import org.cloudstorage.repository.UserRepository;
import org.cloudstorage.service.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("Регистрация - пользователь успешно создан")
    void register_ShouldCreateUser() {
        // проверка на существование
        when(userRepository.existsByUsername("user")).thenReturn(false);
        // кодируем пароль, получаем хеш
        when(passwordEncoder.encode("password")).thenReturn("$2a$10$hashed");

        // Специальный класс-объект-заглушка под объект User.class, подменяет объект,
        // который будет передан в параметры метода
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // Метод save ожидает User объект, мы ему кидаем нашу заглушку имитацию.
        when(userRepository.save(captor.capture())).thenAnswer(invocation -> {
            // Забираем переданный объект, добавляем ему id и возвращаем
            User saved =  invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        User result = userService.register("user", "password");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("user");
        assertThat(result.getPassword()).isEqualTo("$2a$10$hashed");

        // Проверяем что пароль был захеширован
        verify(passwordEncoder).encode("password");

        User capUser = captor.getValue();
        assertThat(capUser.getUsername()).isEqualTo("user");
        assertThat(capUser.getPassword()).isEqualTo("$2a$10$hashed");
    }


    @Test
    @DisplayName("Регистрация - дубликат username должен вызывать исключение")
    void register_DuplicateUsername_ShouldThrowException() {
        // Настраиваем мой на занятый username
        when(userRepository.existsByUsername("user")).thenReturn(true);

        // проверяем что метод выбрасывает исключение
        assertThatThrownBy(() -> userService.register("user", "password"))
                .isInstanceOf(UserAlreadyExistException.class);

        // кодирование пароля и сохранение пользователя не должно работать
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Регистрация - проверка хеширования пароля перед сохранением")
    void register_ShouldHashPasswordBeforeSaving() {
        when(userRepository.existsByUsername("user")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));


        userService.register("user", "password");

        var inOrder = Mockito.inOrder(userRepository, passwordEncoder);
        inOrder.verify(userRepository).existsByUsername("user");
        inOrder.verify(passwordEncoder).encode("password");
        inOrder.verify(userRepository).save(any(User.class));
    }

}
