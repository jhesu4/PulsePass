package com.pulse.pass.controller;

import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.VenueService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VenueController.class)
@WithMockUser(username = "mvc-test-user", roles = "USER")
class VenueControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private VenueService venueService;

    @Test
    @DisplayName("GET /api/venues/active should return active venues")
    void shouldExposeActiveVenuesEndpoint() throws Exception {
        given(venueService.findActiveVenues()).willReturn(List.of(
                new VenueResponse(1L, "VEN-001", "Main Venue", "Santa Marta", "Calle 10", 5000, true)
        ));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("VEN-001"));

        verify(venueService).findActiveVenues();
    }

    @Test
    @DisplayName("GET /api/venues/{code} should return 404 when the venue does not exist")
    void shouldReturnNotFoundWhenVenueDoesNotExist() throws Exception {
        given(venueService.findByCode("VEN-MISSING"))
                .willThrow(new ResourceNotFoundException("Venue not found: VEN-MISSING"));

        mockMvc.perform(get("/api/venues/VEN-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Venue not found: VEN-MISSING"));

        verify(venueService).findByCode("VEN-MISSING");
    }

        @Test
        @DisplayName("GET /api/venues/{code} should return the venue")
        void shouldReturnVenueByCode() throws Exception {
                given(venueService.findByCode("VEN-001")).willReturn(
                                new VenueResponse(1L, "VEN-001", "Main Venue", "Santa Marta", "Calle 10", 5000, true)
                );

                mockMvc.perform(get("/api/venues/VEN-001"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.code").value("VEN-001"));

                verify(venueService).findByCode("VEN-001");
        }
}
