package com.pulse.pass.repository;

import com.pulse.pass.domain.UserCredential;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {

	Optional<UserCredential> findByUser_Id(Long userId);
}