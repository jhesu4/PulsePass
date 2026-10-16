package com.pulse.pass.security;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final ObjectMapper objectMapper;
	private final SecurityAuditLogger securityAuditLogger;

	public RestAccessDeniedHandler(ObjectMapper objectMapper, SecurityAuditLogger securityAuditLogger) {
		this.objectMapper = objectMapper;
		this.securityAuditLogger = securityAuditLogger;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException {
		securityAuditLogger.accessDenied();
		RestSecurityErrorResponseWriter.write(objectMapper, response, HttpStatus.FORBIDDEN,
				"No tiene permisos para realizar esta operación");
	}
}