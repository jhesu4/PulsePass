package com.pulse.pass.mapper;

import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.response.VenueResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    VenueResponse toResponse(Venue venue);
}