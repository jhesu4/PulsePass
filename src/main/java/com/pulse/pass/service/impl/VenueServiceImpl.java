package com.pulse.pass.service.impl;

import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.VenueMapper;
import com.pulse.pass.repository.VenueRepository;
import com.pulse.pass.service.VenueService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VenueServiceImpl implements VenueService {

    private final VenueRepository venueRepository;
    private final VenueMapper venueMapper;

    public VenueServiceImpl(VenueRepository venueRepository, VenueMapper venueMapper) {
        this.venueRepository = venueRepository;
        this.venueMapper = venueMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public VenueResponse findByCode(String code) {
        Venue venue = venueRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + code));
        return venueMapper.toResponse(venue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VenueResponse> findActiveVenues() {
        return venueRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(venueMapper::toResponse)
                .toList();
    }
}