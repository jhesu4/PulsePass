package com.pulse.pass.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.User;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.security.SecurityPrincipal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private EventRepository eventRepository;

	private AuthorizationService authorizationService;

	@BeforeEach
	void setUp() {
		authorizationService = new AuthorizationService(userRepository, ticketRepository, eventRepository);
		SecurityContextHolder.clearContext();
		RequestContextHolder.resetRequestAttributes();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void shouldMatchCurrentUserByEmailAndUsername() {
		User user = userWithId(42L);
		when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(user));
		when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
		authenticate(42L, "USER");

		assertThat(authorizationService.isCurrentUserEmail("andrea@email.com")).isTrue();
		assertThat(authorizationService.isCurrentUserUsername("andrea")).isTrue();
		assertThat(authorizationService.canPurchaseForEmail("andrea@email.com")).isTrue();
	}

	@Test
	void shouldNotMatchOtherOrMissingUsersWithoutAuthentication() {
		User otherUser = userWithId(9L);
		when(userRepository.findByEmailIgnoreCase("other@email.com"))
				.thenReturn(Optional.of(otherUser));
		when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());
		authenticate(42L, "USER");

		assertThat(authorizationService.isCurrentUserEmail("other@email.com")).isFalse();
		assertThat(authorizationService.isCurrentUserUsername("missing")).isFalse();

		SecurityContextHolder.clearContext();
		assertThat(authorizationService.isCurrentUserEmail("other@email.com")).isFalse();
	}

	@Test
	void shouldAllowTicketOwnerAndDenyOtherUsers() {
		authenticate(42L, "USER");
		when(ticketRepository.existsByTicketCodeAndUser_Id("TCK-1", 42L)).thenReturn(true);
		when(ticketRepository.existsByTicketCodeAndUser_Id("TCK-2", 42L)).thenReturn(false);

		assertThat(authorizationService.canReadTicket("TCK-1")).isTrue();
		assertThat(authorizationService.canManageTicket("TCK-1")).isTrue();
		assertThat(authorizationService.canReadTicket("TCK-2")).isFalse();
		assertThat(authorizationService.canManageTicket("TCK-2")).isFalse();
	}

	@Test
	void shouldLetStaffReadButNotManageTickets() {
		authenticate(6L, "STAFF");

		assertThat(authorizationService.canReadTicket("TCK-1")).isTrue();
		assertThat(authorizationService.canManageTicket("TCK-1")).isFalse();
		verify(ticketRepository).existsByTicketCodeAndUser_Id("TCK-1", 6L);
	}

	@Test
	void shouldRequireAdminRoleAndApprovedRequestPurpose() {
		authenticate(1L, "ADMIN");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Admin-Purpose", "TICKET_SUPPORT");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		assertThat(authorizationService.hasAdminPurpose("TICKET_SUPPORT")).isTrue();
		assertThat(authorizationService.hasAdminPurpose("CUSTOMER_SUPPORT")).isFalse();
		assertThat(authorizationService.hasAdminPurpose("UNAPPROVED_PURPOSE")).isFalse();

		SecurityContextHolder.clearContext();
		authenticate(2L, "USER");
		assertThat(authorizationService.hasAdminPurpose("TICKET_SUPPORT")).isFalse();
	}

	@Test
	void shouldAllowAdminTicketAccessOnlyWithTicketSupportPurpose() {
		authenticate(1L, "ADMIN");
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		assertThat(authorizationService.canReadTicket("TCK-1")).isFalse();
		assertThat(authorizationService.canManageTicket("TCK-1")).isFalse();

		request.addHeader("X-Admin-Purpose", "TICKET_SUPPORT");
		assertThat(authorizationService.canReadTicket("TCK-1")).isTrue();
		assertThat(authorizationService.canManageTicket("TCK-1")).isTrue();
		assertThat(authorizationService.canReadTicketsForEmail("owner@email.com")).isTrue();
	}

	@Test
	void shouldExposeOnlyPublishedEventsToRegularUsers() {
		Event publishedEvent = org.mockito.Mockito.mock(Event.class);
		Event draftEvent = org.mockito.Mockito.mock(Event.class);
		when(eventRepository.findByEventCode("EVT-PUBLISHED")).thenReturn(Optional.of(publishedEvent));
		when(eventRepository.findByEventCode("EVT-DRAFT")).thenReturn(Optional.of(draftEvent));
		when(publishedEvent.getStatus()).thenReturn(EventStatus.PUBLISHED);
		when(draftEvent.getStatus()).thenReturn(EventStatus.DRAFT);

		assertThat(authorizationService.canViewEvent("EVT-PUBLISHED")).isTrue();
		assertThat(authorizationService.canViewEvent("EVT-DRAFT")).isFalse();
		assertThat(authorizationService.canViewEvent("EVT-MISSING")).isFalse();
	}

	@Test
	void shouldAllowStaffAndAdminsToViewAnyEvent() {
		authenticate(6L, "STAFF");
		assertThat(authorizationService.canViewEvent("EVT-DRAFT")).isTrue();
		assertThat(authorizationService.isStaffOrAdmin()).isTrue();
		verify(eventRepository, never()).findByEventCode("EVT-DRAFT");

		SecurityContextHolder.clearContext();
		authenticate(1L, "ADMIN");
		assertThat(authorizationService.canViewEvent("EVT-MISSING")).isTrue();
		assertThat(authorizationService.isStaffOrAdmin()).isTrue();
	}

	private User userWithId(Long id) {
		User user = org.mockito.Mockito.mock(User.class);
		when(user.getId()).thenReturn(id);
		return user;
	}

	private void authenticate(Long userId, String... roles) {
		SecurityPrincipal principal = new SecurityPrincipal(userId, "user-" + userId, "hash", true,
				java.util.Arrays.stream(roles)
						.map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
						.toList());
		SecurityContextHolder.getContext().setAuthentication(
				UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
	}
}