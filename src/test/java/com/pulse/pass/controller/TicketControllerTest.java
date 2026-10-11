package com.pulse.pass.controller;

import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.dto.request.PurchaseTicketRequest;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.exception.GlobalExceptionHandler;
import com.pulse.pass.service.TicketService;
import java.math.BigDecimal;
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

class TicketControllerTest {

    @Test
    @DisplayName("POST /api/tickets/purchase should purchase a ticket")
    void shouldPurchaseTicket() throws Exception {
        TicketService ticketService = mock(TicketService.class);
        TicketResponse response = new TicketResponse(
                1L,
                "TCK-001",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.PAID,
                LocalDateTime.now(),
                "john@pulse.com",
                "EVT-001",
                "Music Festival"
        );

        given(ticketService.purchase(any(PurchaseTicketRequest.class))).willReturn(response);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TicketController(ticketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/tickets/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"john@pulse.com\",\"eventCode\":\"EVT-001\",\"type\":\"GENERAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("TCK-001"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketCode} should return a ticket")
    void shouldReturnTicketByCode() throws Exception {
        TicketService ticketService = mock(TicketService.class);
        given(ticketService.findByCode("TCK-001")).willReturn(
                new TicketResponse(1L, "TCK-001", TicketType.VIP, new BigDecimal("300000.00"), TicketStatus.PAID, LocalDateTime.now(), "john@pulse.com", "EVT-001", "Music Festival")
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TicketController(ticketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/tickets/TCK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-001"));

        verify(ticketService).findByCode("TCK-001");
    }

    @Test
    @DisplayName("GET /api/tickets/by-user should match the PRD contract")
    void shouldExposeByUserAliasEndpoint() throws Exception {
        TicketService ticketService = mock(TicketService.class);
        given(ticketService.findByUserEmail("john@pulse.com")).willReturn(List.of(
                new TicketResponse(1L, "TCK-001", TicketType.GENERAL, new BigDecimal("150000.00"), TicketStatus.PAID, LocalDateTime.now(), "john@pulse.com", "EVT-001", "Music Festival")
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TicketController(ticketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/tickets/by-user").param("email", "john@pulse.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-001"));

        verify(ticketService).findByUserEmail("john@pulse.com");
    }

    @Test
    @DisplayName("GET /api/tickets/by-user without email parameter should return 400")
    void shouldReturnBadRequestWhenEmailParameterIsMissing() throws Exception {
        TicketService ticketService = mock(TicketService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TicketController(ticketService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/tickets/by-user"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
