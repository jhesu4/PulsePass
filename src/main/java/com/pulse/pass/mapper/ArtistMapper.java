package com.pulse.pass.mapper;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.dto.response.ArtistResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtistMapper {

    ArtistResponse toResponse(Artist artist);
}