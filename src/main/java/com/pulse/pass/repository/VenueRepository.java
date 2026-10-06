package com.pulse.pass.repository;

import com.pulse.pass.domain.Venue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VenueRepository extends JpaRepository<Venue, Long> {

	Optional<Venue> findByCode(String code);

	List<Venue> findByActiveTrueOrderByNameAsc();
}
