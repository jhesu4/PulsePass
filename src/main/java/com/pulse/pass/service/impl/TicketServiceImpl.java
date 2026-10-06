package com.pulse.pass.service.impl;

import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserProfile;
import com.pulse.pass.dto.request.PurchaseTicketRequest;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.TicketMapper;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.service.TicketService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;
    private final TicketPriceCalculator ticketPriceCalculator;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            TicketMapper ticketMapper,
            TicketPriceCalculator ticketPriceCalculator) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.ticketPriceCalculator = ticketPriceCalculator;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));
        if (!user.isActive()) {
            throw new BusinessRuleException("Inactive user cannot purchase tickets: " + request.userEmail());
        }

        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Tickets can only be purchased for PUBLISHED events: " + request.eventCode());
        }
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot purchase tickets for an event that has already occurred: " + request.eventCode());
        }

        validateMinimumAge(user.getProfile(), event);

        long paidTickets = ticketRepository.countByEventCodeAndStatus(event.getEventCode(), TicketStatus.PAID);
        int capacity = event.getVenue().getCapacity();
        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no remaining capacity: " + request.eventCode());
        }

        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-" + UUID.randomUUID());
        ticket.setType(request.type());
        ticket.setPrice(ticketPriceCalculator.calculatePrice(request.type()));
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user);
        ticket.setEvent(event);

        Ticket savedTicket = ticketRepository.save(ticket);
        if (paidTickets + 1 == capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }
        return ticketMapper.toResponse(savedTicket);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email).stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findPaidTicketsByEventCode(eventCode).stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be cancelled: " + ticketCode);
        }
        if (!ticket.getEvent().getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot cancel a ticket after the event date: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be marked as used: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    private void validateMinimumAge(UserProfile profile, Event event) {
        if (event.getMinimumAge() == 0) {
            return;
        }
        if (profile == null || profile.getBirthDate() == null) {
            throw new BusinessRuleException("Birth date is required for this event: " + event.getEventCode());
        }

        LocalDate eventDate = event.getEventDate().toLocalDate();
        int ageAtEvent = Period.between(profile.getBirthDate(), eventDate).getYears();
        if (ageAtEvent < event.getMinimumAge()) {
            throw new BusinessRuleException("User does not meet minimum age for event: " + event.getEventCode());
        }
    }
}