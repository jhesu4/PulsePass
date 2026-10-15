package com.pulse.pass.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private CustomUserDetailsService userDetailsService;

	@Mock
	private RestAuthenticationEntryPoint authenticationEntryPoint;

	@Mock
	private FilterChain filterChain;

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		filter = new JwtAuthenticationFilter(jwtService, userDetailsService, authenticationEntryPoint);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldContinueWithoutAuthorizationHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(authenticationEntryPoint, never()).commence(
				org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
				org.mockito.ArgumentMatchers.any(HttpServletResponse.class),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void shouldContinueWhenAuthorizationSchemeIsNotBearer() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Basic abc");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(jwtService, never()).validateAccessToken(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void shouldAuthenticateEnabledPrincipalFromBearerToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "bEaReR valid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		Claims claims = org.mockito.Mockito.mock(Claims.class);
		SecurityPrincipal principal = new SecurityPrincipal(42L, "andrea", "hash", true,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
		when(jwtService.validateAccessToken("valid-token")).thenReturn(claims);
		when(claims.getSubject()).thenReturn("42");
		when(userDetailsService.loadUserById(42L)).thenReturn(principal);

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(principal);
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
				.extracting("authority").containsExactly("ROLE_USER");
		verify(filterChain).doFilter(request, response);
		verify(authenticationEntryPoint, never()).commence(
				org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
				org.mockito.ArgumentMatchers.any(HttpServletResponse.class),
				org.mockito.ArgumentMatchers.any());
	}

	@Test
	void shouldRejectMalformedBearerHeadersWithoutContinuing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(authenticationEntryPoint).commence(
				org.mockito.ArgumentMatchers.eq(request),
				org.mockito.ArgumentMatchers.eq(response),
				org.mockito.ArgumentMatchers.any(BadCredentialsException.class));
		verify(filterChain, never()).doFilter(request, response);
		verify(jwtService, never()).validateAccessToken(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void shouldRejectMultipleAuthorizationHeadersWithoutOverwritingContext() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer valid-token");
		request.addHeader("Authorization", "Bearer another-token");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, filterChain);

		verify(authenticationEntryPoint).commence(
				org.mockito.ArgumentMatchers.eq(request),
				org.mockito.ArgumentMatchers.eq(response),
				org.mockito.ArgumentMatchers.any(BadCredentialsException.class));
		verify(filterChain, never()).doFilter(request, response);
		verify(jwtService, never()).validateAccessToken(org.mockito.ArgumentMatchers.anyString());
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void shouldNotOverwriteAnExistingAuthentication() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer valid-token");
		MockHttpServletResponse response = new MockHttpServletResponse();
		UsernamePasswordAuthenticationToken existing = UsernamePasswordAuthenticationToken.authenticated(
				"existing-principal", "password", List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContextHolder.getContext().setAuthentication(existing);

		filter.doFilter(request, response, filterChain);

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
		verify(jwtService, never()).validateAccessToken(org.mockito.ArgumentMatchers.anyString());
		verify(authenticationEntryPoint, never()).commence(
				org.mockito.ArgumentMatchers.any(HttpServletRequest.class),
				org.mockito.ArgumentMatchers.any(HttpServletResponse.class),
				org.mockito.ArgumentMatchers.any());
		verify(filterChain).doFilter(request, response);
	}

	@Test
	void shouldRejectInvalidSubjectAndDisabledPrincipal() throws Exception {
		MockHttpServletRequest invalidSubjectRequest = new MockHttpServletRequest();
		invalidSubjectRequest.addHeader("Authorization", "Bearer invalid-subject");
		Claims invalidSubjectClaims = org.mockito.Mockito.mock(Claims.class);
		when(jwtService.validateAccessToken("invalid-subject")).thenReturn(invalidSubjectClaims);
		when(invalidSubjectClaims.getSubject()).thenReturn("not-a-user-id");

		filter.doFilter(invalidSubjectRequest, new MockHttpServletResponse(), filterChain);

		verify(authenticationEntryPoint).commence(
				org.mockito.ArgumentMatchers.eq(invalidSubjectRequest),
				org.mockito.ArgumentMatchers.any(HttpServletResponse.class),
				org.mockito.ArgumentMatchers.any(BadCredentialsException.class));
		verify(userDetailsService, never()).loadUserById(org.mockito.ArgumentMatchers.anyLong());
		verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

		SecurityContextHolder.clearContext();
		MockHttpServletRequest disabledRequest = new MockHttpServletRequest();
		disabledRequest.addHeader("Authorization", "Bearer disabled-user");
		Claims disabledClaims = org.mockito.Mockito.mock(Claims.class);
		when(jwtService.validateAccessToken("disabled-user")).thenReturn(disabledClaims);
		when(disabledClaims.getSubject()).thenReturn("7");
		when(userDetailsService.loadUserById(7L)).thenReturn(
				new SecurityPrincipal(7L, "disabled", "hash", false, List.of()));

		filter.doFilter(disabledRequest, new MockHttpServletResponse(), filterChain);

		verify(authenticationEntryPoint).commence(
				org.mockito.ArgumentMatchers.eq(disabledRequest),
				org.mockito.ArgumentMatchers.any(HttpServletResponse.class),
				org.mockito.ArgumentMatchers.any(BadCredentialsException.class));
		verify(filterChain, never()).doFilter(
				org.mockito.ArgumentMatchers.eq(disabledRequest), org.mockito.ArgumentMatchers.any());
	}
}