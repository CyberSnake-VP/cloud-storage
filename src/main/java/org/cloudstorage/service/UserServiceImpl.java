package org.cloudstorage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cloudstorage.exception.UserAlreadyExistException;
import org.cloudstorage.model.User;
import org.cloudstorage.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    // PasswordEncoder — это интерфейс. Spring Security предоставит реализацию BCryptPasswordEncoder.
    // Мы создадим этот бин в SecurityConfig.
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public User register(String username, String password) {
        // проверка уникальности
        if(userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistException(
                    String.format("Username %s is already in use", username)
            );
        }

        // создаем нового пользователя
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password)) // хешируем пароль
                .build();

        // сохраняем пользователя
        User saved =  userRepository.save(user);
        log.info("User registered successfully: {}", saved.getUsername());
        return saved;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(()-> new RuntimeException(String.format("User %s not found", username)));
    }
}
