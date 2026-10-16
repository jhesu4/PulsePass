package com.pulse.pass.security;

import java.io.IOException;
import java.util.Enumeration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final CustomUserDetailsService userDetailsService;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;

	public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService,
			RestAuthenticationEntryPoint authenticationEntryPoint) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.authenticationEntryPoint = authenticationEntryPoint;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		Enumeration<String> authorizationHeaders = request.getHeaders("Authorization");
		if (!authorizationHeaders.hasMoreElements()) {
			filterChain.doFilter(request, response);
			return;
		}

		String authorizationHeader = authorizationHeaders.nextElement();
		if (authorizationHeaders.hasMoreElements()) {
			reject(request, response, new BadCredentialsException("Invalid authorization header"));
			return;
		}

		if (!authorizationHeader.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
			filterChain.doFilter(request, response);
			return;
		}
		if (authorizationHeader.length() <= "Bearer ".length()
				|| authorizationHeader.charAt("Bearer".length()) != ' ') {
			reject(request, response, new BadCredentialsException("Invalid authorization header"));
			return;
		}

		String token = authorizationHeader.substring("Bearer ".length()).trim();
		if (token.isEmpty()) {
			reject(request, response, new BadCredentialsException("Invalid authorization header"));
			return;
		}

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				String subject = jwtService.validateAccessToken(token).getSubject();
				long userId = Long.parseLong(subject);
				if (userId <= 0) {
					throw new BadCredentialsException("Invalid access token");
				}

				SecurityPrincipal principal = userDetailsService.loadUserById(userId);
				if (!principal.isEnabled()) {
					throw new BadCredentialsException("Invalid access token");
				}
				UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
						principal, null, principal.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			} catch (AuthenticationException | NumberFormatException exception) {
				reject(request, response, exception);
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private void reject(HttpServletRequest request, HttpServletResponse response, Exception exception)
			throws IOException, ServletException {
		authenticationEntryPoint.commence(request, response,
				exception instanceof AuthenticationException authenticationException
						? authenticationException
						: new BadCredentialsException("Invalid access token", exception));
	}
}