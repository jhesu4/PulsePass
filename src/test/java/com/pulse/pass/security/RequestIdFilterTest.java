package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class RequestIdFilterTest {

	@Test
	void shouldRejectInsecureRemoteRequestsWhenHttpsIsRequired() throws Exception {
		SecurityAuditLogger auditLogger = mock(SecurityAuditLogger.class);
		RequestIdFilter filter = new RequestIdFilter(JsonMapper.builder().build(), auditLogger, true);
		MockHttpServletRequest request = remoteHttpRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean chainInvoked = new AtomicBoolean();

		filter.doFilter(request, response, (servletRequest, servletResponse) -> chainInvoked.set(true));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("HTTPS is required");
		assertThat(chainInvoked).isFalse();
		verify(auditLogger).transportRejected();
	}

	@Test
	void shouldAllowInsecureRemoteRequestsWhenHttpsRequirementIsDisabled() throws Exception {
		SecurityAuditLogger auditLogger = mock(SecurityAuditLogger.class);
		RequestIdFilter filter = new RequestIdFilter(JsonMapper.builder().build(), auditLogger, false);
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean chainInvoked = new AtomicBoolean();

		filter.doFilter(remoteHttpRequest(), response,
				(servletRequest, servletResponse) -> chainInvoked.set(true));

		assertThat(response.getStatus()).isEqualTo(200);
		assertThat(chainInvoked).isTrue();
		verifyNoInteractions(auditLogger);
	}

	private MockHttpServletRequest remoteHttpRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("198.51.100.10");
		return request;
	}
}