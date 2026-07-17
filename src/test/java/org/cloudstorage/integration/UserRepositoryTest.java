package org.cloudstorage.integration;

import org.cloudstorage.model.User;
import org.cloudstorage.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    // СОХРАНЕНИЕ
    @Test
    @DisplayName("save - новый пользователь сохраняется с ID")
    void save_NewUser_ShouldReturnedWithGeneratedID() {
        User user = User.builder()
                .username("username")
                .password("password")
                .build();

        User saved = userRepository.save(user);

        // flush + clear = записываем в БД и стираем кеш, чтобы find() не брал данные из кеша, а из БД
        entityManager.flush();
        entityManager.clear();

        // Проверяем что ID сгенерирован (не null)
        assertThat(saved.getId()).isNotNull();

        // Проверяем что запись реально в БД
        User fromDb = entityManager.find(User.class, saved.getId());
        assertThat(fromDb).isNotNull();
        assertThat(fromDb.getUsername()).isEqualTo("username");
    }

    // СУЩЕСТВОВАНИЕ
    @Test
    @DisplayName("existsByUsername - пользователь существует")
    void existByUsername_ShouldReturnTrue() {
         User user = User.builder()
                 .username("existing")
                 .password("password")
                 .build();

         entityManager.persist(user);
         entityManager.flush();

         assertThat(userRepository.existsByUsername("existing")).isTrue();
         assertThat(userRepository.existsByUsername("username")).isFalse();
    }

    // УНИКАЛЬНОСТЬ
    @Test
    @DisplayName("Уникальность username - дубликат вызывает ошибку")
    void save_DuplicateUsername_ShouldThrowException() {
        // Сохраняем первого пользователя
        User user1 = User.builder()
                .username("unique")
                .password("pass1")
                .build();
        entityManager.persist(user1);
        entityManager.flush();

        // Попытка сохранить пользователя с тем же username
        User user2 = User.builder()
                .username("unique")
                .password("pass2")
                .build();

        assertThatThrownBy(() -> {
            userRepository.save(user2);
            entityManager.flush(); // Тут бд выбрасывает исключение
        }).isInstanceOf(DataIntegrityViolationException.class);

    }

}
