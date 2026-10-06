package com.pulse.pass.mapper;

import com.pulse.pass.domain.Event;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = ArtistMapper.class)
public interface EventMapper {

    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    EventResponse toResponse(Event event);

    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    EventSummaryResponse toSummary(Event event);
}