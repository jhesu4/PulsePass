package com.pulse.pass.controller;

import com.pulse.pass.dto.request.LoginRequest;
import com.pulse.pass.dto.response.AuthenticatedUserResponse;
import com.pulse.pass.dto.response.AuthResponse;
import com.pulse.pass.security.SecurityPrincipal;
import com.pulse.pass.service.AuthService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@GetMapping("/me")
	public ResponseEntity<AuthenticatedUserResponse> currentUser(
			@AuthenticationPrincipal SecurityPrincipal principal) {
		List<String> roles = principal.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.map(authority -> authority.startsWith("ROLE_") ? authority.substring(5) : authority)
				.sorted()
				.toList();
		return ResponseEntity.ok(new AuthenticatedUserResponse(principal.getUserId(), principal.getUsername(), roles));
	}
}