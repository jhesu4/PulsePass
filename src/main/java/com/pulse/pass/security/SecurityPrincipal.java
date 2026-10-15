package com.pulse.pass.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityPrincipal implements UserDetails {

	private final Long userId;
	private final String username;
	private final String passwordHash;
	private final boolean enabled;
	private final List<GrantedAuthority> authorities;

	public SecurityPrincipal(Long userId, String username, String passwordHash, boolean enabled,
			List<GrantedAuthority> authorities) {
		this.userId = userId;
		this.username = username;
		this.passwordHash = passwordHash;
		this.enabled = enabled;
		this.authorities = List.copyOf(authorities);
	}

	public Long getUserId() {
		return userId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}