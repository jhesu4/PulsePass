package com.pulse.pass.service;

import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.security.SecurityPrincipal;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service("authorizationService")
public class AuthorizationService {

	private final UserRepository userRepository;
	private final TicketRepository ticketRepository;
	private final EventRepository eventRepository;

	public AuthorizationService(UserRepository userRepository, TicketRepository ticketRepository,
			EventRepository eventRepository) {
		this.userRepository = userRepository;
		this.ticketRepository = ticketRepository;
		this.eventRepository = eventRepository;
	}

	public boolean isCurrentUserEmail(String email) {
		Long requestedUserId = userRepository.findByEmailIgnoreCase(email).map(user -> user.getId()).orElse(null);
		return isCurrentUser(requestedUserId);
	}

	public boolean isCurrentUserUsername(String username) {
		Long requestedUserId = userRepository.findByUsername(username).map(user -> user.getId()).orElse(null);
		return isCurrentUser(requestedUserId);
	}

	public boolean canPurchaseForEmail(String email) {
		return isCurrentUserEmail(email);
	}

	public boolean canReadTicket(String ticketCode) {
		return hasRole("STAFF") || ownsTicket(ticketCode) || hasAdminPurpose("TICKET_SUPPORT");
	}

	public boolean canManageTicket(String ticketCode) {
		return ownsTicket(ticketCode) || hasAdminPurpose("TICKET_SUPPORT");
	}

	public boolean canReadTicketsForEmail(String email) {
		return isCurrentUserEmail(email) || hasAdminPurpose("TICKET_SUPPORT");
	}

	public boolean hasAdminPurpose(String expectedPurpose) {
		if (!hasRole("ADMIN") || !Set.of("CUSTOMER_SUPPORT", "TICKET_SUPPORT").contains(expectedPurpose)) {
			return false;
		}

		var requestAttributes = RequestContextHolder.getRequestAttributes();
		if (!(requestAttributes instanceof ServletRequestAttributes servletAttributes)
				|| !expectedPurpose.equals(servletAttributes.getRequest().getHeader("X-Admin-Purpose"))) {
			return false;
		}

		return true;
	}

	public boolean canViewEvent(String eventCode) {
		if (hasRole("STAFF") || hasRole("ADMIN")) {
			return true;
		}
		return eventRepository.findByEventCode(eventCode)
				.map(event -> event.getStatus() == EventStatus.PUBLISHED)
				.orElse(false);
	}

	public boolean isStaffOrAdmin() {
		return hasRole("STAFF") || hasRole("ADMIN");
	}

	private boolean ownsTicket(String ticketCode) {
		SecurityPrincipal principal = currentPrincipal();
		return principal != null
				&& ticketRepository.existsByTicketCodeAndUser_Id(ticketCode, principal.getUserId());
	}

	private boolean isCurrentUser(Long requestedUserId) {
		SecurityPrincipal principal = currentPrincipal();
		return principal != null && Objects.equals(principal.getUserId(), requestedUserId);
	}

	private SecurityPrincipal currentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof SecurityPrincipal principal)) {
			return null;
		}
		return principal;
	}

	private boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
	}
}