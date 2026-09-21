package com.pulse.pass.repository;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.TestcontainersConfiguration;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserProfile;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserProfileTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    @DisplayName("FR-USR-001 / FR-USR-004: Registrar usuario, crear perfil y recuperar vía User.getProfile()")
    void shouldSaveUserAndRetrieveProfile() {
        User user = userRepository.save(new User("johndoe", "john@pulse.com", true));

        UserProfile profile = new UserProfile();
        profile.setFirstName("John");
        profile.setLastName("Doe");
        profile.setPhone("3001234567");
        profile.setCity("Santa Marta");
        profile.setBirthDate(LocalDate.of(1995, 8, 15));
        profile.setUser(user);
        user.setProfile(profile); // Se sincroniza la relación en memoria
        userProfileRepository.saveAndFlush(profile);

        User foundUser = userRepository.findByUsername("johndoe").orElse(null);
        assertThat(foundUser).isNotNull();
        assertThat(foundUser.getProfile()).isNotNull();
        assertThat(foundUser.getProfile().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("FR-USR-002: Rechazar usuarios con email duplicado")
    void shouldRejectDuplicateEmail() {
        userRepository.saveAndFlush(new User("user1", "user1@pulse.com", true));

        User dupEmail = new User("user2", "user1@pulse.com", true);
        assertThatThrownBy(() -> userRepository.saveAndFlush(dupEmail))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-USR-002: Rechazar usuarios con username duplicado")
    void shouldRejectDuplicateUsername() {
        userRepository.saveAndFlush(new User("user1", "user1@pulse.com", true));

        User dupUsername = new User("user1", "user2@pulse.com", true);
        assertThatThrownBy(() -> userRepository.saveAndFlush(dupUsername))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-USR-003 / AC-004: PostgreSQL debe rechazar un segundo perfil para el mismo usuario (Relación 1:1)")
    void shouldRejectDuplicateProfileForSameUser() {
        User user = userRepository.save(new User("user_unique", "unique@pulse.com", true));

        UserProfile profile1 = new UserProfile();
        profile1.setFirstName("Carlos");
        profile1.setLastName("Gómez");
        profile1.setCity("Santa Marta");
        profile1.setBirthDate(LocalDate.of(1998, 5, 20));
        profile1.setUser(user);
        userProfileRepository.saveAndFlush(profile1);

        UserProfile profile2 = new UserProfile();
        profile2.setFirstName("Juan");
        profile2.setLastName("Pérez");
        profile2.setCity("Barranquilla");
        profile2.setBirthDate(LocalDate.of(2000, 10, 10));
        profile2.setUser(user);

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(profile2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}