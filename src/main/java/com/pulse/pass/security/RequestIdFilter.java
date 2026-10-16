package com.pulse.pass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

public class RequestIdFilter extends OncePerRequestFilter {

	public static final String HEADER_NAME = "X-Request-ID";
	private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
	private final ObjectMapper objectMapper;
	private final SecurityAuditLogger securityAuditLogger;
	private final boolean requireHttps;

	public RequestIdFilter(ObjectMapper objectMapper, SecurityAuditLogger securityAuditLogger, boolean requireHttps) {
		this.objectMapper = objectMapper;
		this.securityAuditLogger = securityAuditLogger;
		this.requireHttps = requireHttps;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = resolveRequestId(request);
		MDC.put("requestId", requestId);
		response.setHeader(HEADER_NAME, requestId);
		try {
			if (requireHttps && !request.isSecure() && !isLocalRequest(request)) {
				securityAuditLogger.transportRejected();
				RestSecurityErrorResponseWriter.write(objectMapper, response, HttpStatus.FORBIDDEN,
						"HTTPS is required");
				return;
			}
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove("requestId");
		}
	}

	private String resolveRequestId(HttpServletRequest request) {
		Enumeration<String> requestIds = request.getHeaders(HEADER_NAME);
		if (!requestIds.hasMoreElements()) {
			return UUID.randomUUID().toString();
		}

		String requestId = requestIds.nextElement();
		if (requestIds.hasMoreElements() || !VALID_REQUEST_ID.matcher(requestId).matches()) {
			return UUID.randomUUID().toString();
		}
		return requestId;
	}

	private boolean isLocalRequest(HttpServletRequest request) {
		String remoteAddress = request.getRemoteAddr();
		return "::1".equalsIgnoreCase(remoteAddress)
				|| "0:0:0:0:0:0:0:1".equalsIgnoreCase(remoteAddress)
				|| "::ffff:127.0.0.1".equalsIgnoreCase(remoteAddress)
				|| remoteAddress != null && remoteAddress.startsWith("127.");
	}
}