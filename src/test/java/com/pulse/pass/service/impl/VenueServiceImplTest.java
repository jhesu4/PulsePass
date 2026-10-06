package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.VenueMapper;
import com.pulse.pass.repository.VenueRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueMapper venueMapper;

    private VenueServiceImpl venueService;

    @BeforeEach
    void setUp() {
        venueService = new VenueServiceImpl(venueRepository, venueMapper);
    }

    @Test
    void shouldReturnVenueWhenCodeExists() {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 # 22-10", 3, true);
        VenueResponse response = new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 # 22-10", 3, true);

        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(response);

        VenueResponse result = venueService.findByCode("VEN-SMR-01");

        assertThat(result).isNotNull();
        assertThat(result.code()).isEqualTo("VEN-SMR-01");
        verify(venueRepository).findByCode("VEN-SMR-01");
    }

    @Test
    void shouldThrowExceptionWhenVenueCodeDoesNotExist() {
        when(venueRepository.findByCode("VEN-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> venueService.findByCode("VEN-404"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found: VEN-404");

        verify(venueRepository).findByCode("VEN-404");
    }

    @Test
    void shouldReturnOnlyActiveVenues() {
        Venue venue = new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 # 22-10", 3, true);
        VenueResponse response = new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Cra 1 # 22-10", 3, true);

        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(response);

        List<VenueResponse> result = venueService.findActiveVenues();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("VEN-SMR-01");
        verify(venueRepository).findByActiveTrueOrderByNameAsc();
    }
}