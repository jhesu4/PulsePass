package com.pulse.pass.service;

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
import com.pulse.pass.security.SecurityPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private final UserService userService;
	private final UserRepository userRepository;
	private final UserCredentialRepository userCredentialRepository;
	private final UserRoleRepository userRoleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;

	public AuthService(UserService userService, UserRepository userRepository,
			UserCredentialRepository userCredentialRepository, UserRoleRepository userRoleRepository,
			PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
		this.userService = userService;
		this.userRepository = userRepository;
		this.userCredentialRepository = userCredentialRepository;
		this.userRoleRepository = userRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
	}

	@Transactional
	public UserResponse register(RegisterUserRequest request) {
		UserResponse response = userService.register(request);
		User user = userRepository.findById(response.id()).orElseThrow();

		UserCredential credential = new UserCredential(user, passwordEncoder.encode(request.password()));
		userCredentialRepository.save(credential);
		userRoleRepository.save(new UserRole(user, UserRoleType.USER));

		return response;
	}

	public SecurityPrincipal authenticate(LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				UsernamePasswordAuthenticationToken.unauthenticated(request.identifier(), request.password()));
		return (SecurityPrincipal) authentication.getPrincipal();
	}

	public AuthResponse login(LoginRequest request) {
		SecurityPrincipal principal = authenticate(request);
		return new AuthResponse(jwtService.createAccessToken(principal.getUserId()), "Bearer",
				jwtService.getAccessTokenTtlSeconds());
	}
}