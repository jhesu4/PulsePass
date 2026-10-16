package com.pulse.pass.security;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleType;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserCredentialRepository userCredentialRepository;

	@Mock
	private UserRoleRepository userRoleRepository;

	@InjectMocks
	private CustomUserDetailsService userDetailsService;

	private User user;

	@BeforeEach
	void setUp() {
		user = mock(User.class);
	}

	@Test
	void shouldLoadPrincipalAndRolesByUsername() {
		when(user.getId()).thenReturn(42L);
		when(user.getUsername()).thenReturn("andrea");
		when(user.isActive()).thenReturn(true);
		when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
		when(userCredentialRepository.findByUser_Id(42L)).thenReturn(Optional.of(credential("$2a$12$hash")));
		UserRole role = new UserRole(user, UserRoleType.USER);
		when(userRoleRepository.findAllByUser_Id(42L)).thenReturn(List.of(role));

		SecurityPrincipal principal = (SecurityPrincipal) userDetailsService.loadUserByUsername("andrea");

		assertThat(principal.getUserId()).isEqualTo(42L);
		assertThat(principal.getUsername()).isEqualTo("andrea");
		assertThat(principal.getPassword()).isEqualTo("$2a$12$hash");
		assertThat(principal.isEnabled()).isTrue();
		assertThat(principal.getAuthorities()).extracting(authority -> authority.getAuthority())
				.containsExactly("ROLE_USER");
	}

	@Test
	void shouldLoadPrincipalByEmail() {
		when(user.getId()).thenReturn(42L);
		when(user.getUsername()).thenReturn("andrea");
		when(user.isActive()).thenReturn(true);
		when(userRepository.findByEmailIgnoreCase("andrea@example.com")).thenReturn(Optional.of(user));
		when(userCredentialRepository.findByUser_Id(42L)).thenReturn(Optional.of(credential("$2a$12$hash")));
		UserRole role = new UserRole(user, UserRoleType.USER);
		when(userRoleRepository.findAllByUser_Id(42L)).thenReturn(List.of(role));

		SecurityPrincipal principal = (SecurityPrincipal) userDetailsService.loadUserByUsername("andrea@example.com");

		assertThat(principal.getUsername()).isEqualTo("andrea");
	}

	@Test
	void shouldRejectUnknownUser() {
		when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
				.isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void shouldRejectLegacyUserWithoutCredential() {
		when(user.getId()).thenReturn(42L);
		when(userRepository.findByUsername("legacy")).thenReturn(Optional.of(user));
		when(userCredentialRepository.findByUser_Id(42L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("legacy"))
				.isInstanceOf(UsernameNotFoundException.class);
	}

	@Test
	void shouldMarkInactiveUserAsDisabledAndLoadCurrentRoles() {
		when(user.getId()).thenReturn(42L);
		when(user.getUsername()).thenReturn("andrea");
		when(user.isActive()).thenReturn(false);
		when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
		when(userCredentialRepository.findByUser_Id(42L)).thenReturn(Optional.of(credential("$2a$12$hash")));
		UserRole role = new UserRole(user, UserRoleType.STAFF);
		when(userRoleRepository.findAllByUser_Id(42L)).thenReturn(List.of(role));

		SecurityPrincipal principal = (SecurityPrincipal) userDetailsService.loadUserByUsername("andrea");

		assertThat(principal.isEnabled()).isFalse();
		assertThat(principal.getAuthorities()).extracting(authority -> authority.getAuthority())
				.containsExactly("ROLE_STAFF");
	}

	private UserCredential credential(String passwordHash) {
		return new UserCredential(user, passwordHash);
	}
}