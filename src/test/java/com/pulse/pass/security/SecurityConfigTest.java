package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
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
}