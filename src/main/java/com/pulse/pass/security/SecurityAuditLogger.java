package com.pulse.pass.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuditLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditLogger.class);

	public void loginSucceeded(Long userId) {
		LOGGER.info("security_event=login_success user_id={} request_id={}", userId, requestId());
	}

	public void loginFailed() {
		LOGGER.warn("security_event=login_failure request_id={}", requestId());
	}

	public void authenticationRejected() {
		LOGGER.warn("security_event=authentication_rejected request_id={}", requestId());
	}

	public void accessDenied() {
		LOGGER.warn("security_event=access_denied user_id={} request_id={}", currentUserId(), requestId());
	}

	public void transportRejected() {
		LOGGER.warn("security_event=transport_rejected request_id={}", requestId());
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
			return principal.getUserId();
		}
		return null;
	}

	private String requestId() {
		return MDC.get("requestId");
	}
}