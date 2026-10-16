package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.pulse.pass.dto.request.ValidPasswordValidator;

class ValidPasswordValidatorTest {

	private final ValidPasswordValidator validator = new ValidPasswordValidator();

	@Test
	void shouldAcceptAtLeastTwelveCharactersWithinBcryptByteLimit() {
		assertThat(validator.isValid("123456789012", null)).isTrue();
		assertThat(validator.isValid("é".repeat(36), null)).isTrue();
	}

	@Test
	void shouldRejectPasswordsShorterThanTwelveCharacters() {
		assertThat(validator.isValid("12345678901", null)).isFalse();
	}

	@Test
	void shouldRejectPasswordsOverSeventyTwoUtf8Bytes() {
		assertThat(validator.isValid("é".repeat(37), null)).isFalse();
	}
}