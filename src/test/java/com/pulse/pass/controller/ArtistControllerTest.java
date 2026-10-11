package com.pulse.pass.controller;

import com.pulse.pass.dto.response.ArtistResponse;
import com.pulse.pass.exception.GlobalExceptionHandler;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.ArtistService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArtistControllerTest {

    @Test
    @DisplayName("GET /api/artists/active should return active artists")
    void shouldExposeActiveArtistsEndpoint() throws Exception {
        ArtistService artistService = mock(ArtistService.class);
        given(artistService.findActiveArtists())
                .willReturn(List.of(new ArtistResponse(1L, "Solar Beat", "Spain", "Electronic", true)));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ArtistController(artistService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"));

        verify(artistService).findActiveArtists();
    }

    @Test
    @DisplayName("GET /api/artists/by-stage-name should match the PRD contract")
    void shouldExposeByStageNameAliasEndpoint() throws Exception {
        ArtistService artistService = mock(ArtistService.class);
        given(artistService.findByStageName("Solar Beat"))
                .willReturn(new ArtistResponse(1L, "Solar Beat", "Spain", "Electronic", true));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ArtistController(artistService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    @DisplayName("GET /api/artists/{id} should return 404 when the artist does not exist")
    void shouldReturnNotFoundWhenArtistDoesNotExist() throws Exception {
        ArtistService artistService = mock(ArtistService.class);
        given(artistService.findById(99L)).willThrow(new ResourceNotFoundException("Artist 99 not found"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ArtistController(artistService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/artists/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Artist 99 not found"));

        verify(artistService).findById(99L);
    }
}
