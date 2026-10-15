package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class SecurityAuditLoggerTest {

	private final SecurityAuditLogger securityAuditLogger = new SecurityAuditLogger();

	@AfterEach
	void clearRequestId() {
		MDC.remove("requestId");
	}

	@Test
	void shouldLogSecurityOutcomesWithRequestIdWithoutSecrets(CapturedOutput output) {
		MDC.put("requestId", "audit-request-42");
		securityAuditLogger.loginSucceeded(42L);
		securityAuditLogger.loginFailed();
		securityAuditLogger.authenticationRejected();
		securityAuditLogger.accessDenied();

		assertThat(output.getOut())
				.contains("security_event=login_success")
				.contains("security_event=login_failure")
				.contains("security_event=authentication_rejected")
				.contains("security_event=access_denied")
				.contains("audit-request-42")
				.contains("user_id=42")
				.doesNotContain("UnaFraseSeguraLarga2026!", "signed-test-token", "passwordHash");
	}
}