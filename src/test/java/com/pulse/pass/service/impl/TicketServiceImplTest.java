package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserProfile;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.request.PurchaseTicketRequest;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.TicketMapper;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketPriceCalculator ticketPriceCalculator;

    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(
                ticketRepository, userRepository, eventRepository, ticketMapper, ticketPriceCalculator);
    }

    @Test
    void shouldPurchasePaidTicketWithCalculatedPrice() {
        User user = user(true, LocalDate.now().minusYears(30));
        Event event = event(EventStatus.PUBLISHED, 3, 18, LocalDateTime.now().plusDays(30));
        PurchaseTicketRequest request = request(TicketType.VIP);
        TicketResponse response = response(TicketStatus.PAID);
        stubPurchase(user, event, 0, request);
        when(ticketPriceCalculator.calculatePrice(TicketType.VIP)).thenReturn(new BigDecimal("150000.00"));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0, Ticket.class));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response);

        assertThat(ticketService.purchase(request)).isSameAs(response);

        verify(ticketRepository).save(org.mockito.ArgumentMatchers.argThat(ticket ->
                ticket.getStatus() == TicketStatus.PAID
                        && ticket.getPrice().equals(new BigDecimal("150000.00"))
                        && ticket.getTicketCode().matches("TCK-[0-9a-fA-F-]{36}")));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldRejectPurchaseWhenUserDoesNotExist() {
        PurchaseTicketRequest request = request(TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase(request.userEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchase(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: andrea@email.com");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldRejectPurchaseForInactiveUser() {
        PurchaseTicketRequest request = request(TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase(request.userEmail())).thenReturn(Optional.of(user(false, null)));

        assertThatThrownBy(() -> ticketService.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Inactive user cannot purchase tickets: andrea@email.com");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldRejectPurchaseWhenEventDoesNotExist() {
        PurchaseTicketRequest request = request(TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase(request.userEmail())).thenReturn(Optional.of(user(true, null)));
        when(eventRepository.findByEventCode(request.eventCode())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.purchase(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: EVT-1");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldRejectPurchaseWhenEventIsNotPublished() {
        PurchaseTicketRequest request = request(TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase(request.userEmail())).thenReturn(Optional.of(user(true, null)));
        when(eventRepository.findByEventCode(request.eventCode()))
                .thenReturn(Optional.of(event(EventStatus.DRAFT, 3, 0, LocalDateTime.now().plusDays(30))));

        assertThatThrownBy(() -> ticketService.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Tickets can only be purchased for PUBLISHED events: EVT-1");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldRejectPurchaseForPastEvent() {
        PurchaseTicketRequest request = request(TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase(request.userEmail())).thenReturn(Optional.of(user(true, null)));
        when(eventRepository.findByEventCode(request.eventCode()))
                .thenReturn(Optional.of(event(EventStatus.PUBLISHED, 3, 0, LocalDateTime.now().minusDays(1))));

        assertThatThrownBy(() -> ticketService.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot purchase tickets for an event that has already occurred: EVT-1");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldRejectPurchaseWhenUserDoesNotMeetMinimumAgeAtEventDate() {
        User user = user(true, LocalDate.now().minusYears(16));
        Event event = event(EventStatus.PUBLISHED, 3, 18, LocalDateTime.now().plusDays(30));
        PurchaseTicketRequest request = request(TicketType.GENERAL);
        when(userRepository.findByEmailIgnoreCase(request.userEmail())).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(request.eventCode())).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("User does not meet minimum age for event: EVT-1");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldRejectPurchaseWhenCapacityIsFull() {
        User user = user(true, LocalDate.now().minusYears(30));
        Event event = event(EventStatus.PUBLISHED, 2, 0, LocalDateTime.now().plusDays(30));
        PurchaseTicketRequest request = request(TicketType.GENERAL);
        stubPurchase(user, event, 2, request);

        assertThatThrownBy(() -> ticketService.purchase(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Event has no remaining capacity: EVT-1");

        verify(ticketRepository, never()).save(any(Ticket.class));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void shouldMarkEventSoldOutWhenLastTicketIsPurchased() {
        User user = user(true, LocalDate.now().minusYears(30));
        Event event = event(EventStatus.PUBLISHED, 1, 0, LocalDateTime.now().plusDays(30));
        PurchaseTicketRequest request = request(TicketType.GENERAL);
        stubPurchase(user, event, 0, request);
        when(ticketPriceCalculator.calculatePrice(TicketType.GENERAL)).thenReturn(new BigDecimal("100000.00"));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0, Ticket.class));
        when(eventRepository.save(event)).thenReturn(event);
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(response(TicketStatus.PAID));

        ticketService.purchase(request);

        assertThat(event.getStatus()).isEqualTo(EventStatus.SOLD_OUT);
        verify(eventRepository).save(event);
    }

    @Test
    void shouldCancelPaidTicketBeforeEvent() {
        Ticket ticket = ticket(TicketStatus.PAID, event(EventStatus.PUBLISHED, 10, 0, LocalDateTime.now().plusDays(1)));
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response(TicketStatus.CANCELLED));

        assertThat(ticketService.cancel("TCK-1").status()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.CANCELLED);
    }

    @Test
    void shouldRejectCancellingUsedTicket() {
        when(ticketRepository.findByTicketCode("TCK-1"))
                .thenReturn(Optional.of(ticket(TicketStatus.USED, event(EventStatus.PUBLISHED, 10, 0, LocalDateTime.now().plusDays(1)))));

        assertThatThrownBy(() -> ticketService.cancel("TCK-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only PAID tickets can be cancelled: TCK-1");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldRejectCancellingTicketAfterEventDate() {
        when(ticketRepository.findByTicketCode("TCK-1"))
                .thenReturn(Optional.of(ticket(TicketStatus.PAID, event(EventStatus.FINISHED, 10, 0, LocalDateTime.now().minusDays(1)))));

        assertThatThrownBy(() -> ticketService.cancel("TCK-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot cancel a ticket after the event date: TCK-1");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldMarkPaidTicketAsUsed() {
        Ticket ticket = ticket(TicketStatus.PAID, event(EventStatus.PUBLISHED, 10, 0, LocalDateTime.now().plusDays(1)));
        when(ticketRepository.findByTicketCode("TCK-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response(TicketStatus.USED));

        assertThat(ticketService.markAsUsed("TCK-1").status()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);
    }

    @Test
    void shouldRejectUsingCancelledTicket() {
        when(ticketRepository.findByTicketCode("TCK-1"))
                .thenReturn(Optional.of(ticket(TicketStatus.CANCELLED, event(EventStatus.PUBLISHED, 10, 0, LocalDateTime.now().plusDays(1)))));

        assertThatThrownBy(() -> ticketService.markAsUsed("TCK-1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only PAID tickets can be marked as used: TCK-1");

        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void shouldReturnNotFoundWhenTicketCodeDoesNotExist() {
        when(ticketRepository.findByTicketCode("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.findByCode("MISSING"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ticket not found: MISSING");
    }

    private void stubPurchase(User user, Event event, long paidCount, PurchaseTicketRequest request) {
        when(userRepository.findByEmailIgnoreCase(request.userEmail())).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode(request.eventCode())).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID)).thenReturn(paidCount);
    }

    private PurchaseTicketRequest request(TicketType type) {
        return new PurchaseTicketRequest("andrea@email.com", "EVT-1", type);
    }

    private User user(boolean active, LocalDate birthDate) {
        User user = new User("andrea", "andrea@email.com", active);
        UserProfile profile = new UserProfile();
        profile.setBirthDate(birthDate);
        profile.setUser(user);
        user.setProfile(profile);
        return user;
    }

    private Event event(EventStatus status, int capacity, int minimumAge, LocalDateTime eventDate) {
        Event event = new Event();
        event.setEventCode("EVT-1");
        event.setName("Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(status);
        event.setEventDate(eventDate);
        event.setMinimumAge(minimumAge);
        event.setVenue(new Venue("VEN-1", "Venue", "City", "Address", capacity, true));
        return event;
    }

    private Ticket ticket(TicketStatus status, Event event) {
        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-1");
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(new BigDecimal("100000.00"));
        ticket.setStatus(status);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user(true, LocalDate.now().minusYears(30)));
        ticket.setEvent(event);
        return ticket;
    }

    private TicketResponse response(TicketStatus status) {
        return new TicketResponse(
                1L, "TCK-1", TicketType.GENERAL, new BigDecimal("100000.00"), status,
                LocalDateTime.now(), "andrea@email.com", "EVT-1", "Event");
    }
}