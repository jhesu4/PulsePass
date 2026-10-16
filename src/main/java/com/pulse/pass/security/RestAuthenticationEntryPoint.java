package com.pulse.pass.security;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;
	private final SecurityAuditLogger securityAuditLogger;

	public RestAuthenticationEntryPoint(ObjectMapper objectMapper, SecurityAuditLogger securityAuditLogger) {
		this.objectMapper = objectMapper;
		this.securityAuditLogger = securityAuditLogger;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		securityAuditLogger.authenticationRejected();
		RestSecurityErrorResponseWriter.write(objectMapper, response, HttpStatus.UNAUTHORIZED,
				"Invalid credentials or access token");
	}
}