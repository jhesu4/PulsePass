package com.pulse.pass.controller;

import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.dto.request.PurchaseTicketRequest;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.TicketService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@WithMockUser(username = "mvc-test-user", roles = "USER")
class TicketControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private TicketService ticketService;

    @Test
    @DisplayName("POST /api/tickets should purchase a ticket")
    void shouldPurchaseTicket() throws Exception {
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

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"john@pulse.com\",\"eventCode\":\"EVT-001\",\"type\":\"GENERAL\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("TCK-001"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @DisplayName("POST /api/tickets/purchase should not expose the legacy route")
    void shouldNotExposeLegacyPurchaseRoute() throws Exception {
        mockMvc.perform(post("/api/tickets/purchase"))
                .andExpect(status().isNotFound());

        verify(ticketService, never()).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketCode} should return a ticket")
    void shouldReturnTicketByCode() throws Exception {
        given(ticketService.findByCode("TCK-001")).willReturn(
                new TicketResponse(1L, "TCK-001", TicketType.VIP, new BigDecimal("300000.00"), TicketStatus.PAID, LocalDateTime.now(), "john@pulse.com", "EVT-001", "Music Festival")
        );

        mockMvc.perform(get("/api/tickets/TCK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-001"));

        verify(ticketService).findByCode("TCK-001");
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketCode} should accept generated UUID codes")
    void shouldReturnTicketByGeneratedUuidCode() throws Exception {
        String ticketCode = "TCK-550e8400-e29b-41d4-a716-446655440000";
        given(ticketService.findByCode(ticketCode)).willReturn(
                new TicketResponse(1L, ticketCode, TicketType.GENERAL, new BigDecimal("150000.00"),
                        TicketStatus.PAID, LocalDateTime.now(), "john@pulse.com", "EVT-001", "Music Festival")
        );

        mockMvc.perform(get("/api/tickets/" + ticketCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value(ticketCode));

        verify(ticketService).findByCode(ticketCode);
    }

    @Test
        @DisplayName("GET /api/tickets/by-user should return tickets for the user")
        void shouldReturnTicketsByUser() throws Exception {
        given(ticketService.findByUserEmail("john@pulse.com")).willReturn(List.of(
                new TicketResponse(1L, "TCK-001", TicketType.GENERAL, new BigDecimal("150000.00"), TicketStatus.PAID, LocalDateTime.now(), "john@pulse.com", "EVT-001", "Music Festival")
        ));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "john@pulse.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-001"));

        verify(ticketService).findByUserEmail("john@pulse.com");
    }

    @Test
    @DisplayName("GET /api/tickets/by-user without email parameter should return 400")
    void shouldReturnBadRequestWhenEmailParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/tickets/by-user"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.email").value("Required request parameter is missing"));

        verify(ticketService, never()).findByUserEmail(anyString());
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketCode} should return 404 when the ticket does not exist")
    void shouldReturnNotFoundWhenTicketDoesNotExist() throws Exception {
        given(ticketService.findByCode("TCK-MISSING"))
                .willThrow(new ResourceNotFoundException("Ticket not found: TCK-MISSING"));

        mockMvc.perform(get("/api/tickets/TCK-MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket not found: TCK-MISSING"));

        verify(ticketService).findByCode("TCK-MISSING");
    }

        @Test
        @DisplayName("POST /api/tickets should return 404 when the user does not exist")
        void shouldReturnNotFoundWhenPurchaseUserDoesNotExist() throws Exception {
                given(ticketService.purchase(any(PurchaseTicketRequest.class)))
                                .willThrow(new ResourceNotFoundException("User not found: john@pulse.com"));

                mockMvc.perform(post("/api/tickets")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"userEmail\":\"john@pulse.com\",\"eventCode\":\"EVT-001\",\"type\":\"GENERAL\"}"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("User not found: john@pulse.com"));

                verify(ticketService).purchase(any(PurchaseTicketRequest.class));
        }

    @Test
    @DisplayName("POST /api/tickets should return 409 when the purchase violates a business rule")
    void shouldReturnConflictWhenTicketCannotBePurchased() throws Exception {
        given(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .willThrow(new BusinessRuleException("Event has no remaining capacity: EVT-001"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"john@pulse.com\",\"eventCode\":\"EVT-001\",\"type\":\"GENERAL\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Event has no remaining capacity: EVT-001"));

        verify(ticketService).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    @DisplayName("POST /api/tickets should return validation details without purchasing")
    void shouldRejectInvalidPurchaseWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userEmail\":\"invalid\",\"eventCode\":\"\",\"type\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.userEmail").value("userEmail must be valid"))
                .andExpect(jsonPath("$.details.eventCode").value("eventCode is required"))
                .andExpect(jsonPath("$.details.type").value("type is required"));

        verify(ticketService, never()).purchase(any(PurchaseTicketRequest.class));
    }

        @Test
        @DisplayName("POST /api/tickets without Content-Type should return 415")
        void shouldReturnUnsupportedMediaTypeWhenContentTypeIsMissing() throws Exception {
                mockMvc.perform(post("/api/tickets")
                                                .content("{\"userEmail\":\"john@pulse.com\",\"eventCode\":\"EVT-001\",\"type\":\"GENERAL\"}"))
                                .andExpect(status().isUnsupportedMediaType())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.status").value(415))
                                .andExpect(jsonPath("$.details.contentType").exists());

                verify(ticketService, never()).purchase(any(PurchaseTicketRequest.class));
        }

    @Test
    @DisplayName("GET /api/tickets/user should not expose the legacy route")
    void shouldNotExposeLegacyUserTicketsRoute() throws Exception {
        mockMvc.perform(get("/api/tickets/user").param("email", "john@pulse.com"))
                .andExpect(status().isNotFound());

        verify(ticketService, never()).findByUserEmail(any(String.class));
    }

    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/cancel should cancel a ticket")
    void shouldCancelTicket() throws Exception {
        given(ticketService.cancel("TCK-001")).willReturn(ticketResponse(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TCK-001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TCK-001");
    }

    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/cancel should return 409 for an invalid transition")
    void shouldReturnConflictWhenTicketCannotBeCancelled() throws Exception {
        given(ticketService.cancel("TCK-001"))
                .willThrow(new BusinessRuleException("Only PAID tickets can be cancelled: TCK-001"));

        mockMvc.perform(patch("/api/tickets/TCK-001/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(ticketService).cancel("TCK-001");
    }

    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/use should mark a ticket as used")
        @WithMockUser(username = "mvc-staff", roles = "STAFF")
    void shouldMarkTicketAsUsed() throws Exception {
        given(ticketService.markAsUsed("TCK-001")).willReturn(ticketResponse(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TCK-001/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TCK-001");
    }

    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/use should return 409 for an invalid transition")
    void shouldReturnConflictWhenTicketCannotBeUsed() throws Exception {
        given(ticketService.markAsUsed("TCK-001"))
                .willThrow(new BusinessRuleException("Cannot use ticket: TCK-001"));

        mockMvc.perform(patch("/api/tickets/TCK-001/use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(ticketService).markAsUsed("TCK-001");
    }

    private TicketResponse ticketResponse(TicketStatus status) {
        return new TicketResponse(
                1L,
                "TCK-001",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                status,
                LocalDateTime.now(),
                "john@pulse.com",
                "EVT-001",
                "Music Festival"
        );
    }
}
