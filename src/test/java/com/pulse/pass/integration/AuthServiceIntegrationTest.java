package com.pulse.pass.integration;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import com.pulse.pass.TestcontainersConfiguration;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleType;
import com.pulse.pass.dto.request.LoginRequest;
import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.AuthResponse;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import com.pulse.pass.security.JwtService;
import com.pulse.pass.security.SecurityPrincipal;
import com.pulse.pass.service.AuthService;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthServiceIntegrationTest {

	private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

	@Autowired
	private AuthService authService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCredentialRepository userCredentialRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void shouldRegisterUserWithBcryptCredentialAndUserRole() {
		var response = authService.register(request("auth_register", "register@pulse.com"));

		User user = userRepository.findById(response.id()).orElseThrow();
		var credential = userCredentialRepository.findByUser_Id(user.getId()).orElseThrow();
		List<UserRole> roles = userRoleRepository.findAllByUser_Id(user.getId());

		assertThat(response.username()).isEqualTo("auth_register");
		assertThat(credential.getPasswordHash()).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, credential.getPasswordHash())).isTrue();
		assertThat(roles).extracting(userRole -> userRole.getRole()).containsExactly(UserRoleType.USER);
	}

	@Test
	void shouldAuthenticateByUsernameAndEmail() {
		var response = authService.register(request("auth_login", "login@pulse.com"));

		SecurityPrincipal byUsername = authService.authenticate(new LoginRequest("auth_login", PASSWORD));
		SecurityPrincipal byEmail = authService.authenticate(new LoginRequest("LOGIN@pulse.com", PASSWORD));

		assertThat(byUsername.getUserId()).isEqualTo(response.id());
		assertThat(byEmail.getUserId()).isEqualTo(response.id());
		assertThat(byUsername.getAuthorities()).extracting(authority -> authority.getAuthority())
				.containsExactly("ROLE_USER");
		assertThatThrownBy(() -> authService.authenticate(new LoginRequest("auth_login", "wrong-password")))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void shouldLoginAndReturnSignedAccessToken() {
		var user = authService.register(request("auth_token", "token@pulse.com"));

		AuthResponse response = authService.login(new LoginRequest("auth_token", PASSWORD));
		var claims = jwtService.validateAccessToken(response.accessToken());

		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(900);
		assertThat(claims.getSubject()).isEqualTo(user.id().toString());
	}

	@Test
	void shouldRejectUnknownAndLegacyUsers() {
		userRepository.saveAndFlush(new User("auth_legacy", "legacy@pulse.com", true));

		assertThatThrownBy(() -> authService.authenticate(new LoginRequest("missing", PASSWORD)))
				.isInstanceOf(BadCredentialsException.class);
		assertThatThrownBy(() -> authService.authenticate(new LoginRequest("auth_legacy", PASSWORD)))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void shouldRejectInactiveUser() {
		var response = authService.register(request("auth_inactive", "inactive@pulse.com"));
		User user = userRepository.findById(response.id()).orElseThrow();
		user.setActive(false);
		userRepository.flush();

		assertThatThrownBy(() -> authService.authenticate(new LoginRequest("auth_inactive", PASSWORD)))
				.isInstanceOf(DisabledException.class);
	}

	@Test
	void shouldAllowAnonymousPublishedEvents() throws Exception {
		mockMvc.perform(get("/api/events/published"))
				.andExpect(status().isOk());
	}

	@Test
	void shouldReturnUniformUnauthorizedForProtectedRequests() throws Exception {
		mockMvc.perform(post("/api/events")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401));

		mockMvc.perform(post("/api/events")
					.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"));
	}

	@Test
	void shouldDenyUserFromAdminOperation() throws Exception {
		String token = tokenFor("http_user", UserRoleType.USER, true);

		mockMvc.perform(post("/api/events")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("Forbidden"))
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void shouldRejectTokenForInactiveUserOnEachRequest() throws Exception {
		String token = tokenFor("http_inactive", UserRoleType.ADMIN, false);

		mockMvc.perform(post("/api/events")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void shouldReturnCurrentPrincipalWithoutCredentials() throws Exception {
		String token = tokenFor("auth_me", UserRoleType.USER, true);
		User user = userRepository.findByUsername("auth_me").orElseThrow();

		mockMvc.perform(get("/api/auth/me")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(user.getId()))
				.andExpect(jsonPath("$.username").value("auth_me"))
				.andExpect(jsonPath("$.roles[0]").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void shouldRequireAuthenticationForCurrentPrincipal() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"));
	}

	private String tokenFor(String username, UserRoleType role, boolean active) {
		User user = userRepository.saveAndFlush(new User(username, username + "@pulse.com", active));
		userCredentialRepository.saveAndFlush(new UserCredential(user, "test-password-hash"));
		userRoleRepository.saveAndFlush(new UserRole(user, role));
		return jwtService.createAccessToken(user.getId());
	}

	private RegisterUserRequest request(String username, String email) {
		return new RegisterUserRequest(username, email, "Ada", "Lovelace", null, null,
				LocalDate.of(1990, 12, 10), PASSWORD);
	}
}