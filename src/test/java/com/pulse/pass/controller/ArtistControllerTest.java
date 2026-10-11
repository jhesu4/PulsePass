package com.pulse.pass.controller;

import com.pulse.pass.dto.response.ArtistResponse;
import com.pulse.pass.service.ArtistService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ArtistController(artistService)).build();

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"));
    }
}
