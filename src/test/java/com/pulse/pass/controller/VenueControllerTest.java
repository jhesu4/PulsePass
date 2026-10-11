package com.pulse.pass.controller;

import com.pulse.pass.dto.response.VenueResponse;
import com.pulse.pass.service.VenueService;
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

class VenueControllerTest {

    @Test
    @DisplayName("GET /api/venues/active should return active venues")
    void shouldExposeActiveVenuesEndpoint() throws Exception {
        VenueService venueService = mock(VenueService.class);
        given(venueService.findActiveVenues()).willReturn(List.of(
                new VenueResponse(1L, "VEN-001", "Main Venue", "Santa Marta", "Calle 10", 5000, true)
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new VenueController(venueService)).build();

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("VEN-001"));
    }
}
