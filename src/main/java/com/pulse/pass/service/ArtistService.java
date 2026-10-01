package com.pulse.pass.service;

import com.pulse.pass.dto.response.ArtistResponse;
import java.util.List;

public interface ArtistService {

    ArtistResponse findById(Long id);

    ArtistResponse findByStageName(String stageName);

    List<ArtistResponse> findActiveArtists();
}