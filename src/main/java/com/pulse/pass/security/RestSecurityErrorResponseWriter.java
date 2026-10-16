package com.pulse.pass.security;

import com.pulse.pass.dto.response.ErrorResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;

final class RestSecurityErrorResponseWriter {

	private RestSecurityErrorResponseWriter() {
	}

	static void write(ObjectMapper objectMapper, HttpServletResponse response, HttpStatus status, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(
				LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, Map.of()));
	}
}