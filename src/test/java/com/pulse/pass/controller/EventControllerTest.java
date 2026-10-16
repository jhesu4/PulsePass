package com.pulse.pass.controller;

import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.dto.request.CreateEventRequest;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.dto.response.EventSummaryResponse;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.EventService;
import com.pulse.pass.service.TicketService;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@WithMockUser(username = "mvc-test-user", roles = "USER")
class EventControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private EventService eventService;

        @MockitoBean
        private TicketService ticketService;

    @Test
        @DisplayName("POST /api/events should create an event with an empty description")
        @WithMockUser(username = "mvc-admin", roles = "ADMIN")
    void shouldCreateEvent() throws Exception {
        EventResponse response = new EventResponse(
                1L,
                "EVT-001",
                "Music Festival",
                "Festival description",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.now().plusDays(10),
                18,
                "VEN-001",
                "Main Venue",
                List.of()
        );

        given(eventService.create(any(CreateEventRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventCode\":\"EVT-001\",\"name\":\"Music Festival\",\"description\":\"\",\"category\":\"MUSIC\",\"eventDate\":\"2099-12-31T20:00:00\",\"minimumAge\":18,\"venueCode\":\"VEN-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventCode").value("EVT-001"));

        verify(eventService).create(argThat(request -> request.description().isEmpty()));
    }

    @Test
    @DisplayName("GET /api/events/published should return published events")
    void shouldReturnPublishedEvents() throws Exception {
        given(eventService.findPublishedEvents()).willReturn(List.of());

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(eventService).findPublishedEvents();
    }

    @Test
    @DisplayName("POST /api/events with invalid fields should return 400")
    void shouldRejectInvalidEventRequest() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventCode\":\"\",\"name\":\"\",\"description\":\"\",\"category\":\"MUSIC\",\"eventDate\":\"2020-01-01T20:00:00\",\"minimumAge\":-1,\"venueCode\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details.eventCode").value("eventCode is required"));

        verify(eventService, never()).create(any(CreateEventRequest.class));
    }

    @Test
    @DisplayName("GET /api/events/{eventCode} should return 404 when the event does not exist")
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        given(eventService.findByCode("EVT-404")).willThrow(new ResourceNotFoundException("Event not found"));

        mockMvc.perform(get("/api/events/EVT-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(eventService).findByCode("EVT-404");
    }

    @Test
        @DisplayName("POST /api/events should delegate date rules to the service")
        void shouldDelegateEventDateRuleToService() throws Exception {
        given(eventService.create(any(CreateEventRequest.class)))
                .willThrow(new BusinessRuleException("Event date must be in the future."));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventCode\":\"EVT-001\",\"name\":\"Music Festival\",\"description\":\"Festival description\",\"category\":\"MUSIC\",\"eventDate\":\"2020-01-01T20:00:00\",\"minimumAge\":18,\"venueCode\":\"VEN-001\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Event date must be in the future."));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    @DisplayName("POST /api/events should return 400 for malformed JSON without calling the service")
    void shouldRejectMalformedJsonWithoutCreatingEvent() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Malformed JSON request"))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService, never()).create(any(CreateEventRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/events/{eventCode}/publish should publish an event")
        @WithMockUser(username = "mvc-admin", roles = "ADMIN")
    void shouldPublishEvent() throws Exception {
        given(eventService.publish("EVT-001")).willReturn(eventResponse(EventStatus.PUBLISHED));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/events/EVT-001/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("EVT-001");
    }

    @Test
    @DisplayName("PATCH /api/events/{eventCode}/publish should return 409 for a business conflict")
        @WithMockUser(username = "mvc-admin", roles = "ADMIN")
    void shouldReturnConflictWhenEventCannotBePublished() throws Exception {
        given(eventService.publish("EVT-001"))
                .willThrow(new BusinessRuleException("Only DRAFT events can be published: EVT-001"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/events/EVT-001/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(eventService).publish("EVT-001");
    }

    @Test
    @DisplayName("POST /api/events/{eventCode}/artists/{artistId} should associate an artist")
        @WithMockUser(username = "mvc-admin", roles = "ADMIN")
    void shouldAssociateArtistWithEvent() throws Exception {
        given(eventService.addArtist("EVT-001", 1L)).willReturn(eventResponse(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events/EVT-001/artists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("EVT-001"));

        verify(eventService).addArtist("EVT-001", 1L);
    }

    @Test
    @DisplayName("GET /api/events/by-artist should return matching events")
    void shouldFindEventsByArtist() throws Exception {
        given(eventService.findByArtist("Solar Beat")).willReturn(List.of(
                new EventSummaryResponse(1L, "EVT-001", "Music Festival", EventCategory.MUSIC,
                        EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10), "VEN-001", "Main Venue")
        ));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("EVT-001"));

        verify(eventService).findByArtist("Solar Beat");
    }

    @Test
    @DisplayName("GET /api/events/{eventCode}/tickets/paid should return paid tickets")
        @WithMockUser(username = "mvc-staff", roles = "STAFF")
    void shouldFindPaidTicketsByEvent() throws Exception {
        given(ticketService.findPaidTicketsByEvent("EVT-001")).willReturn(List.of(
                new TicketResponse(1L, "TCK-001", TicketType.GENERAL, new BigDecimal("150000.00"),
                        TicketStatus.PAID, LocalDateTime.now(), "john@pulse.com", "EVT-001", "Music Festival")
        ));

        mockMvc.perform(get("/api/events/EVT-001/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-001"));

        verify(ticketService).findPaidTicketsByEvent("EVT-001");
    }

    @Test
    @DisplayName("PATCH /api/events/{eventCode}/artists/{artistId} should not expose the legacy method")
    void shouldNotExposeLegacyPatchForArtistAssociation() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/events/EVT-001/artists/1"))
                .andExpect(status().isMethodNotAllowed());

        verify(eventService, never()).addArtist("EVT-001", 1L);
    }

    private EventResponse eventResponse(EventStatus status) {
        return new EventResponse(
                1L,
                "EVT-001",
                "Music Festival",
                "Festival description",
                EventCategory.MUSIC,
                status,
                LocalDateTime.now().plusDays(10),
                18,
                "VEN-001",
                "Main Venue",
                List.of()
        );
    }
}
