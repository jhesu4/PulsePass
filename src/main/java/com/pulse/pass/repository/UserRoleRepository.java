package com.pulse.pass.repository;

import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

	List<UserRole> findAllByUser_Id(Long userId);
}