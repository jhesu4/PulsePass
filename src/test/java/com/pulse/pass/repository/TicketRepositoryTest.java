package com.pulse.pass.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.pulse.pass.TestcontainersConfiguration;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.Venue;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    @DisplayName("FR-TKT-001: Rechazar emisión de ticket sin usuario o evento (FK NOT NULL)")
    void shouldRejectTicketWithoutFK() {
        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-NOFK");
        ticket.setPrice(new BigDecimal("100000.00"));
        ticket.setType(TicketType.GENERAL);
        ticket.setStatus(TicketStatus.PAID);

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(ticket))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-002 / AC-005: Rechazar ticketCode duplicado")
    void shouldRejectDuplicateTicketCode() {
        User user = userRepository.save(new User("usr_tkt_dup", "dup@pulse.com", true));
        Venue venue = venueRepository.save(new Venue("VEN-T-DUP", "Arena T", "Cali", "Av 1", 1000, true));
        Event event = eventRepository.save(createEvent("EVT-T-DUP", venue));

        ticketRepository.saveAndFlush(createTicket("TCK-0001", user, event, TicketStatus.PAID, new BigDecimal("100000.00")));

        Ticket dup = createTicket("TCK-0001", user, event, TicketStatus.RESERVED, new BigDecimal("120000.00"));

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(dup))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-003: Rechazar ticket con precio negativo")
    void shouldRejectNegativePrice() {
        User user = userRepository.save(new User("usr_neg", "neg@pulse.com", true));
        Venue venue = venueRepository.save(new Venue("VEN-NEG", "Arena N", "Cali", "Av 1", 1000, true));
        Event event = eventRepository.save(createEvent("EVT-NEG", venue));

        Ticket ticket = createTicket("TCK-NEG", user, event, TicketStatus.PAID, new BigDecimal("-100.00"));

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(ticket))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("FR-TKT-006: Consultar tickets de un usuario por email y filtrando opcionalmente por estado")
    void shouldFindTicketsByUserEmailAndStatus() {
        User user = userRepository.save(new User("andrea_p", "andrea@pulse.com", true));
        Venue venue = venueRepository.save(new Venue("VEN-T6", "Arena T6", "Santa Marta", "Av 2", 1000, true));
        Event event = eventRepository.save(createEvent("EVT-T6", venue));

        ticketRepository.save(createTicket("TCK-A1", user, event, TicketStatus.PAID, new BigDecimal("250000.00")));
        ticketRepository.save(createTicket("TCK-A2", user, event, TicketStatus.RESERVED, new BigDecimal("120000.00")));
        ticketRepository.flush();

        List<Ticket> allTickets = ticketRepository.findByUserEmail("andrea@pulse.com");
        assertThat(allTickets).hasSize(2);

        List<Ticket> paidOnly = ticketRepository.findByUserEmailAndStatus("andrea@pulse.com", TicketStatus.PAID);
        assertThat(paidOnly).hasSize(1);
        assertThat(paidOnly.get(0).getTicketCode()).isEqualTo("TCK-A1");
    }

    @Test
    @DisplayName("FR-TKT-007: Recuperar solo tickets PAID de un evento por eventCode")
    void shouldFindPaidTicketsByEventCode() {
        User user = userRepository.save(new User("carlos_m", "carlos@pulse.com", true));
        Venue venue = venueRepository.save(new Venue("VEN-T7", "Arena T7", "Cali", "Av 3", 2000, true));
        Event event = eventRepository.save(createEvent("EVT-T7", venue));

        ticketRepository.save(createTicket("TCK-P1", user, event, TicketStatus.PAID, new BigDecimal("120000.00")));
        ticketRepository.save(createTicket("TCK-R1", user, event, TicketStatus.RESERVED, new BigDecimal("120000.00")));
        ticketRepository.flush();

        List<Ticket> paidTickets = ticketRepository.findPaidTicketsByEventCode("EVT-T7");
        assertThat(paidTickets).hasSize(1);
        assertThat(paidTickets.get(0).getTicketCode()).isEqualTo("TCK-P1");
    }

    @Test
    @DisplayName("FR-TKT-008 / AC-008: Contar únicamente tickets con el estado especificado (PAID, RESERVED, CANCELLED)")
    void shouldCountTicketsByEventCodeAndStatus() {
        User user = userRepository.save(new User("usr_cnt", "cnt@pulse.com", true));
        Venue venue = venueRepository.save(new Venue("VEN-CNT", "Arena Cnt", "Cali", "Av 4", 2000, true));
        Event event = eventRepository.save(createEvent("EVT-CNT-01", venue));

        ticketRepository.save(createTicket("TCK-C1", user, event, TicketStatus.PAID, new BigDecimal("100000.00")));
        ticketRepository.save(createTicket("TCK-C2", user, event, TicketStatus.PAID, new BigDecimal("100000.00")));
        ticketRepository.save(createTicket("TCK-C3", user, event, TicketStatus.RESERVED, new BigDecimal("100000.00")));
        ticketRepository.save(createTicket("TCK-C4", user, event, TicketStatus.CANCELLED, new BigDecimal("100000.00")));
        ticketRepository.flush();

        long paidCount = ticketRepository.countByEventCodeAndStatus("EVT-CNT-01", TicketStatus.PAID);
        long cancelledCount = ticketRepository.countByEventCodeAndStatus("EVT-CNT-01", TicketStatus.CANCELLED);

        assertThat(paidCount).isEqualTo(2);
        assertThat(cancelledCount).isEqualTo(1);
    }

    @Test
    @DisplayName("FR-SRC-004: Consultar tickets de eventos futuros ordenados cronológicamente")
    void shouldFindTicketsForFutureEvents() {
        User user = userRepository.save(new User("usr_fut", "fut@pulse.com", true));
        Venue venue = venueRepository.save(new Venue("VEN-FUT", "Arena Futuro", "Bogotá", "Av 1", 3000, true));

        Event eLate = createEvent("EVT-LATE", venue);
        eLate.setEventDate(LocalDateTime.now().plusDays(10));
        eventRepository.save(eLate);

        Event eSoon = createEvent("EVT-SOON", venue);
        eSoon.setEventDate(LocalDateTime.now().plusDays(2));
        eventRepository.save(eSoon);

        ticketRepository.save(createTicket("TCK-LATE", user, eLate, TicketStatus.PAID, new BigDecimal("100000.00")));
        ticketRepository.save(createTicket("TCK-SOON", user, eSoon, TicketStatus.PAID, new BigDecimal("100000.00")));
        ticketRepository.flush();

        List<Ticket> tickets = ticketRepository.findTicketsForEventsAfter(LocalDateTime.now());
        assertThat(tickets).extracting(t -> t.getEvent().getEventCode()).containsExactly("EVT-SOON", "EVT-LATE");
    }

    private Event createEvent(String code, Venue venue) {
        Event event = new Event();
        event.setEventCode(code);
        event.setName("Evento " + code);
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setMinimumAge(18);
        event.setVenue(venue);
        return event;
    }

    private Ticket createTicket(String code, User user, Event event, TicketStatus status, BigDecimal price) {
        Ticket ticket = new Ticket();
        ticket.setTicketCode(code);
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(price);
        ticket.setStatus(status);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user);
        ticket.setEvent(event);
        return ticket;
    }
}