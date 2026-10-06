package com.pulse.pass.service;

import com.pulse.pass.dto.response.VenueResponse;
import java.util.List;

public interface VenueService {

    VenueResponse findByCode(String code);

    List<VenueResponse> findActiveVenues();
}