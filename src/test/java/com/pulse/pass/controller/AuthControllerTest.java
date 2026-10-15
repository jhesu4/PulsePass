package com.pulse.pass.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pulse.pass.dto.request.LoginRequest;
import com.pulse.pass.dto.response.AuthResponse;
import com.pulse.pass.service.AuthService;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@Test
	void shouldReturnBearerTokenAfterSuccessfulLogin() throws Exception {
		given(authService.login(any(LoginRequest.class)))
				.willReturn(new AuthResponse("signed-test-token", "Bearer", 900));

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"identifier\":\"andrea\",\"password\":\"UnaFraseSeguraLarga2026!\"}"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.accessToken").value("signed-test-token"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900));

		verify(authService).login(any(LoginRequest.class));
	}

	@Test
	void shouldReturnGenericUnauthorizedForInvalidCredentials() throws Exception {
		given(authService.login(any(LoginRequest.class))).willThrow(new BadCredentialsException("Invalid credentials"));

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"identifier\":\"andrea\",\"password\":\"wrong-password\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Invalid credentials or access token"));

		verify(authService).login(any(LoginRequest.class));
	}

	@Test
	void shouldRejectBlankLoginFields() throws Exception {
		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"identifier\":\" \",\"password\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.details.identifier").value("identifier is required"))
				.andExpect(jsonPath("$.details.password").value("password is required"));

		verify(authService, never()).login(any(LoginRequest.class));
	}
}