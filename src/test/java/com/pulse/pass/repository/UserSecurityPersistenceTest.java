package com.pulse.pass.repository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.TestcontainersConfiguration;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleType;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserSecurityPersistenceTest {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCredentialRepository userCredentialRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Test
	@DisplayName("Persistir credencial usando user_id como PK y FK")
	void shouldPersistCredentialUsingUserIdAsPrimaryKey() {
		User user = userRepository.saveAndFlush(new User("credential_user", "credential@pulse.com", true));
		LocalDateTime changedAt = LocalDateTime.of(2026, 10, 15, 10, 30);

		UserCredential credential = new UserCredential(user, "$2a$12$encoded-password-hash");
		credential.setPasswordChangedAt(changedAt);
		userCredentialRepository.saveAndFlush(credential);

		UserCredential found = userCredentialRepository.findById(user.getId()).orElseThrow();
		assertThat(found.getUserId()).isEqualTo(user.getId());
		assertThat(found.getUser().getId()).isEqualTo(user.getId());
		assertThat(found.getPasswordHash()).isEqualTo("$2a$12$encoded-password-hash");
		assertThat(found.getPasswordChangedAt()).isEqualTo(changedAt);
	}

	@Test
	@DisplayName("Persistir distintos roles con clave compuesta por usuario y rol")
	void shouldPersistMultipleRolesForOneUser() {
		User user = userRepository.saveAndFlush(new User("role_user", "roles@pulse.com", true));

		userRoleRepository.saveAllAndFlush(List.of(
				new UserRole(user, UserRoleType.USER),
				new UserRole(user, UserRoleType.STAFF)));

		List<UserRole> roles = userRoleRepository.findAllByUser_Id(user.getId());
		assertThat(roles).extracting(role -> role.getRole())
				.containsExactlyInAnyOrder(UserRoleType.USER, UserRoleType.STAFF);
		assertThat(roles).allSatisfy(role -> assertThat(role.getId().getUserId()).isEqualTo(user.getId()));
	}

	@Test
	@DisplayName("No asignar credenciales ni roles automáticamente a usuarios legados")
	void shouldLeaveUsersWithoutSecurityRecordsUntilExplicitlyProvisioned() {
		User user = userRepository.saveAndFlush(new User("legacy_user", "legacy@pulse.com", true));

		assertThat(userCredentialRepository.findById(user.getId())).isEmpty();
		assertThat(userRoleRepository.findAllByUser_Id(user.getId())).isEmpty();
	}
}