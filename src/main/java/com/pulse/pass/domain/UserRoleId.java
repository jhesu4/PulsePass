package com.pulse.pass.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class UserRoleId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private UserRoleType role;

	public UserRoleId() {}

	public UserRoleId(Long userId, UserRoleType role) {
		this.userId = userId;
		this.role = role;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public UserRoleType getRole() {
		return role;
	}

	public void setRole(UserRoleType role) {
		this.role = role;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof UserRoleId other)) {
			return false;
		}
		return Objects.equals(userId, other.userId) && role == other.role;
	}

	@Override
	public int hashCode() {
		return Objects.hash(userId, role);
	}
}