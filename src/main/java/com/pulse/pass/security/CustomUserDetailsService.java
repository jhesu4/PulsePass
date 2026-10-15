package com.pulse.pass.security;

import com.pulse.pass.domain.User;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid username or password";

	private final UserRepository userRepository;
	private final UserCredentialRepository userCredentialRepository;
	private final UserRoleRepository userRoleRepository;

	public CustomUserDetailsService(UserRepository userRepository,
			UserCredentialRepository userCredentialRepository, UserRoleRepository userRoleRepository) {
		this.userRepository = userRepository;
		this.userCredentialRepository = userCredentialRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(identifier)
				.or(() -> userRepository.findByEmailIgnoreCase(identifier))
				.orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS_MESSAGE));
		return createPrincipal(user);
	}

	public SecurityPrincipal loadUserById(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS_MESSAGE));
		return createPrincipal(user);
	}

	private SecurityPrincipal createPrincipal(User user) {
		String passwordHash = userCredentialRepository.findByUser_Id(user.getId())
				.map(credential -> credential.getPasswordHash())
				.orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS_MESSAGE));

		List<GrantedAuthority> authorities = userRoleRepository.findAllByUser_Id(user.getId()).stream()
				.map(userRole -> userRole.getRole())
				.map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
				.map(authority -> (GrantedAuthority) authority)
				.toList();

		return new SecurityPrincipal(user.getId(), user.getUsername(), passwordHash, user.isActive(), authorities);
	}
}