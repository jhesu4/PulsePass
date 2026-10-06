package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulse.pass.domain.Artist;
import com.pulse.pass.dto.response.ArtistResponse;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.ArtistMapper;
import com.pulse.pass.repository.ArtistRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private ArtistMapper artistMapper;

    private ArtistServiceImpl artistService;

    @BeforeEach
    void setUp() {
        artistService = new ArtistServiceImpl(artistRepository, artistMapper);
    }

    @Test
    void shouldFindArtistById() {
        Artist artist = artist("Solar Beat");
        ArtistResponse response = response("Solar Beat");
        when(artistRepository.findById(7L)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        assertThat(artistService.findById(7L)).isSameAs(response);
    }

    @Test
    void shouldThrowWhenArtistIdDoesNotExist() {
        when(artistRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findById(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: 7");
    }

    @Test
    void shouldFindArtistByStageNameCaseInsensitively() {
        Artist artist = artist("Solar Beat");
        ArtistResponse response = response("Solar Beat");
        when(artistRepository.findByStageNameIgnoreCase("solar beat")).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(response);

        assertThat(artistService.findByStageName("solar beat")).isSameAs(response);
    }

    @Test
    void shouldThrowWhenStageNameDoesNotExist() {
        when(artistRepository.findByStageNameIgnoreCase("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artistService.findByStageName("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: Unknown");
    }

    @Test
    void shouldReturnActiveArtistsOrderedByStageName() {
        Artist firstArtist = artist("Caribbean Sound");
        Artist secondArtist = artist("Solar Beat");
        ArtistResponse firstResponse = response("Caribbean Sound");
        ArtistResponse secondResponse = response("Solar Beat");
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(firstArtist, secondArtist));
        when(artistMapper.toResponse(firstArtist)).thenReturn(firstResponse);
        when(artistMapper.toResponse(secondArtist)).thenReturn(secondResponse);

        assertThat(artistService.findActiveArtists()).containsExactly(firstResponse, secondResponse);
        verify(artistRepository).findByActiveTrueOrderByStageNameAsc();
    }

    private Artist artist(String stageName) {
        return new Artist(stageName, "Colombia", "Pop", true);
    }

    private ArtistResponse response(String stageName) {
        return new ArtistResponse(7L, stageName, "Colombia", "Pop", true);
    }
}