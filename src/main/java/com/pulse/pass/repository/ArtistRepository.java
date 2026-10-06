package com.pulse.pass.repository;

import com.pulse.pass.domain.Artist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

	Optional<Artist> findByStageName(String stageName);

	Optional<Artist> findByStageNameIgnoreCase(String stageName);

	List<Artist> findByActiveTrueOrderByStageNameAsc();
}
