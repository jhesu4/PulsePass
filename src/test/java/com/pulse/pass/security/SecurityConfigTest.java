package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserRoleType;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

	private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

	@Test
	void shouldEncodePasswordWithBcryptAndVerifyIt() {
		String rawPassword = "UnaFraseSeguraLarga2026!";

		String passwordHash = passwordEncoder.encode(rawPassword);

		assertThat(passwordHash).startsWith("$2");
		assertThat(passwordHash).isNotEqualTo(rawPassword);
		assertThat(passwordEncoder.matches(rawPassword, passwordHash)).isTrue();
		assertThat(passwordEncoder.matches("wrong-password", passwordHash)).isFalse();
	}

	@Test
	void shouldAssignConfiguredPrivilegedRoleToAnExistingUser() throws Exception {
		Environment environment = mock(Environment.class);
		UserRepository userRepository = mock(UserRepository.class);
		UserRoleRepository userRoleRepository = mock(UserRoleRepository.class);
		User user = mock(User.class);
		when(environment.getRequiredProperty("pulsepass.security.bootstrap.email")).thenReturn("admin@pulse.com");
		when(environment.getRequiredProperty("pulsepass.security.bootstrap.role")).thenReturn("admin");
		when(userRepository.findByEmailIgnoreCase("admin@pulse.com")).thenReturn(Optional.of(user));
		when(user.getId()).thenReturn(42L);
		when(userRoleRepository.findAllByUser_Id(42L)).thenReturn(List.of());

		new SecurityConfig().localRoleBootstrapRunner(environment, userRepository, userRoleRepository)
				.run(new DefaultApplicationArguments(new String[0]));

		verify(userRoleRepository).save(argThat(userRole -> userRole.getUser() == user
				&& userRole.getRole() == UserRoleType.ADMIN));
	}

	@Test
	void shouldRejectDefaultUserRoleForBootstrap() {
		Environment environment = mock(Environment.class);
		when(environment.getRequiredProperty("pulsepass.security.bootstrap.email")).thenReturn("user@pulse.com");
		when(environment.getRequiredProperty("pulsepass.security.bootstrap.role")).thenReturn("USER");

		assertThatThrownBy(() -> new SecurityConfig()
				.localRoleBootstrapRunner(environment, mock(UserRepository.class), mock(UserRoleRepository.class))
				.run(new DefaultApplicationArguments(new String[0])))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Local role bootstrap only supports STAFF or ADMIN");
	}
}