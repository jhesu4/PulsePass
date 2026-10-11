package com.pulse.pass.controller;

import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.dto.request.CreateEventRequest;
import com.pulse.pass.dto.response.EventResponse;
import com.pulse.pass.exception.GlobalExceptionHandler;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.EventService;
import com.pulse.pass.service.TicketService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest {

    @Test
    @DisplayName("POST /api/events should create event")
    void shouldCreateEvent() throws Exception {
        EventService eventService = mock(EventService.class);
        TicketService ticketService = mock(TicketService.class);
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

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService, ticketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventCode\":\"EVT-001\",\"name\":\"Music Festival\",\"description\":\"Festival description\",\"category\":\"MUSIC\",\"eventDate\":\"2099-12-31T20:00:00\",\"minimumAge\":18,\"venueCode\":\"VEN-001\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventCode").value("EVT-001"));

        verify(eventService).create(any(CreateEventRequest.class));
    }

    @Test
    @DisplayName("GET /api/events/published should return published events")
    void shouldReturnPublishedEvents() throws Exception {
        EventService eventService = mock(EventService.class);
        TicketService ticketService = mock(TicketService.class);
        given(eventService.findPublishedEvents()).willReturn(List.of());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService, ticketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(eventService).findPublishedEvents();
    }

    @Test
    @DisplayName("POST /api/events with invalid fields should return 400")
    void shouldRejectInvalidEventRequest() throws Exception {
        EventService eventService = mock(EventService.class);
        TicketService ticketService = mock(TicketService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService, ticketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventCode\":\"\",\"name\":\"\",\"description\":\"\",\"category\":\"MUSIC\",\"eventDate\":\"2020-01-01T20:00:00\",\"minimumAge\":-1,\"venueCode\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("GET /api/events/{eventCode} should return 404 when the event does not exist")
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        EventService eventService = mock(EventService.class);
        TicketService ticketService = mock(TicketService.class);
        given(eventService.findByCode("EVT-404")).willThrow(new ResourceNotFoundException("Event not found"));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new EventController(eventService, ticketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/events/EVT-404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(eventService).findByCode("EVT-404");
    }
}
