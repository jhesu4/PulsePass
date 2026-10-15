package com.pulse.pass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleType;
import com.pulse.pass.dto.request.LoginRequest;
import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.AuthResponse;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import com.pulse.pass.security.JwtService;
import com.pulse.pass.security.SecurityAuditLogger;
import com.pulse.pass.security.SecurityPrincipal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

	@Mock
	private UserService userService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserCredentialRepository userCredentialRepository;

	@Mock
	private UserRoleRepository userRoleRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtService jwtService;

	@Mock
	private SecurityAuditLogger securityAuditLogger;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(userService, userRepository, userCredentialRepository, userRoleRepository,
				passwordEncoder, authenticationManager, jwtService, securityAuditLogger);
	}

	@Test
	void shouldRegisterUserWithEncodedCredentialAndDefaultRole() {
		RegisterUserRequest request = new RegisterUserRequest("andrea", "andrea@email.com", "Andrea", "Lopez",
				"3001234567", "Santa Marta", LocalDate.of(2001, 5, 10), PASSWORD);
		UserResponse response = new UserResponse(17L, "andrea", "andrea@email.com", "Andrea", "Lopez",
				"3001234567", "Santa Marta", LocalDate.of(2001, 5, 10), true);
		User user = new User("andrea", "andrea@email.com", true);
		when(userService.register(request)).thenReturn(response);
		when(userRepository.findById(17L)).thenReturn(Optional.of(user));
		when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");

		assertThat(authService.register(request)).isSameAs(response);

		verify(userCredentialRepository).save(argThat(credential -> credential.getUser() == user
				&& credential.getPasswordHash().equals("encoded-password")));
		verify(userRoleRepository).save(argThat(role -> role.getUser() == user
				&& role.getRole() == UserRoleType.USER));
	}

	@Test
	void shouldAuthenticateAndRecordSuccessfulLogin() {
		SecurityPrincipal principal = principal();
		when(authenticationManager.authenticate(any())).thenReturn(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

		assertThat(authService.authenticate(new LoginRequest("andrea", PASSWORD))).isSameAs(principal);

		verify(securityAuditLogger).loginSucceeded(42L);
		verify(securityAuditLogger, never()).loginFailed();
	}

	@Test
	void shouldAuditFailedAuthenticationAndRethrowFailure() {
		BadCredentialsException failure = new BadCredentialsException("Invalid credentials");
		when(authenticationManager.authenticate(any())).thenThrow(failure);

		assertThatThrownBy(() -> authService.authenticate(new LoginRequest("andrea", "wrong-password")))
				.isSameAs(failure);

		verify(securityAuditLogger).loginFailed();
		verify(securityAuditLogger, never()).loginSucceeded(any());
	}

	@Test
	void shouldReturnBearerTokenAndConfiguredTtlOnLogin() {
		SecurityPrincipal principal = principal();
		when(authenticationManager.authenticate(any())).thenReturn(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
		when(jwtService.createAccessToken(42L)).thenReturn("signed-token");
		when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

		AuthResponse response = authService.login(new LoginRequest("andrea", PASSWORD));

		assertThat(response).isEqualTo(new AuthResponse("signed-token", "Bearer", 900L));
		verify(securityAuditLogger).loginSucceeded(42L);
	}

	private SecurityPrincipal principal() {
		return new SecurityPrincipal(42L, "andrea", "encoded-password", true,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}
}