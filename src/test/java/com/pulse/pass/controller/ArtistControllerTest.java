package com.pulse.pass.controller;

import com.pulse.pass.dto.response.ArtistResponse;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.ArtistService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
@WithMockUser(username = "mvc-test-user", roles = "USER")
class ArtistControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ArtistService artistService;

    @Test
    @DisplayName("GET /api/artists/active should return active artists")
    void shouldExposeActiveArtistsEndpoint() throws Exception {
        given(artistService.findActiveArtists())
                .willReturn(List.of(new ArtistResponse(1L, "Solar Beat", "Spain", "Electronic", true)));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"));

        verify(artistService).findActiveArtists();
    }

    @Test
        @DisplayName("GET /api/artists/by-stage-name should return an artist")
        void shouldReturnArtistByStageName() throws Exception {
        given(artistService.findByStageName("Solar Beat"))
                .willReturn(new ArtistResponse(1L, "Solar Beat", "Spain", "Electronic", true));

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    @DisplayName("GET /api/artists/{id} should return an artist")
    void shouldReturnArtistById() throws Exception {
        given(artistService.findById(1L))
                .willReturn(new ArtistResponse(1L, "Solar Beat", "Spain", "Electronic", true));

        mockMvc.perform(get("/api/artists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findById(1L);
    }

    @Test
    @DisplayName("GET /api/artists/{id} should return 404 when the artist does not exist")
    void shouldReturnNotFoundWhenArtistDoesNotExist() throws Exception {
        given(artistService.findById(99L)).willThrow(new ResourceNotFoundException("Artist 99 not found"));

        mockMvc.perform(get("/api/artists/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Artist 99 not found"));

        verify(artistService).findById(99L);
    }

    @Test
        @DisplayName("GET /api/artists/{id} should return 404 for a non-numeric route")
        void shouldNotMatchNonNumericArtistRoute() throws Exception {
        mockMvc.perform(get("/api/artists/not-a-number"))
                                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.status").value(404));

        verify(artistService, never()).findById(anyLong());
    }

        @Test
        @DisplayName("GET /api/artists/search should not expose the legacy route")
        void shouldNotExposeLegacySearchRoute() throws Exception {
                mockMvc.perform(get("/api/artists/search").param("stageName", "Solar Beat"))
                                .andExpect(status().isNotFound());

                verify(artistService, never()).findByStageName(anyString());
        }

    @Test
    @DisplayName("GET /api/artists/by-stage-name without a parameter should return 400")
    void shouldRejectMissingStageNameWithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/artists/by-stage-name"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.details.stageName").value("Required request parameter is missing"));

        verify(artistService, never()).findByStageName(anyString());
    }
}
