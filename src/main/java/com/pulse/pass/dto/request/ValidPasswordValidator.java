package com.pulse.pass.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class ValidPasswordValidator implements ConstraintValidator<ValidPassword, String> {

	@Override
	public boolean isValid(String password, ConstraintValidatorContext context) {
		if (password == null) {
			return true;
		}

		int characterCount = password.codePointCount(0, password.length());
		int byteCount = password.getBytes(StandardCharsets.UTF_8).length;
		return characterCount >= 12 && byteCount <= 72;
	}
}