package com.pulse.pass.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pulse.pass.TestcontainersConfiguration;
import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.Ticket;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.TicketType;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleType;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.TicketRepository;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import com.pulse.pass.repository.VenueRepository;
import com.pulse.pass.security.JwtService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SecurityAuthorizationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCredentialRepository userCredentialRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private VenueRepository venueRepository;

	@Autowired
	private ArtistRepository artistRepository;

	@Autowired
	private JwtService jwtService;

	@Test
	void shouldAllowTicketOwnerAndDenyAnotherUserBeforeCancellation() throws Exception {
		User owner = createUser("ticket_owner", UserRoleType.USER);
		User attacker = createUser("ticket_attacker", UserRoleType.USER);
		Ticket ticket = createTicket("TCK-OWNER-1", owner);

		mockMvc.perform(get("/api/tickets/{ticketCode}", ticket.getTicketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(attacker)))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", ticket.getTicketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(attacker)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/tickets/{ticketCode}", ticket.getTicketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", ticket.getTicketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(owner)))
				.andExpect(status().isOk());

		assert ticketRepository.findByTicketCode(ticket.getTicketCode()).orElseThrow().getStatus()
				== TicketStatus.CANCELLED;
	}

	@Test
	void shouldRejectPurchaseForAnotherUsersEmail() throws Exception {
		User buyer = createUser("buyer", UserRoleType.USER);
		createUser("purchase_target", UserRoleType.USER);

		mockMvc.perform(post("/api/tickets")
					.header(HttpHeaders.AUTHORIZATION, bearer(buyer))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"userEmail\":\"purchase_target@pulse.com\","
							+ "\"eventCode\":\"MISSING\",\"type\":\"GENERAL\"}"))
				.andExpect(status().isForbidden());

		org.assertj.core.api.Assertions.assertThat(ticketRepository
				.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc("purchase_target@pulse.com")).isEmpty();
	}

	@Test
	void shouldRestrictUserLookupToTheAuthenticatedIdentity() throws Exception {
		User currentUser = createUser("lookup_current", UserRoleType.USER);
		createUser("lookup_other", UserRoleType.USER);

		mockMvc.perform(get("/api/users/by-email")
					.param("email", "lookup_other@pulse.com")
					.header(HttpHeaders.AUTHORIZATION, bearer(currentUser)))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/users/by-email")
					.param("email", "LOOKUP_CURRENT@pulse.com")
					.header(HttpHeaders.AUTHORIZATION, bearer(currentUser)))
				.andExpect(status().isOk());
	}

	@Test
	void shouldExposePublishedEventsButNotDraftsByCode() throws Exception {
		createEvent("EVT-PUBLIC", EventStatus.PUBLISHED, null);
		createEvent("EVT-DRAFT", EventStatus.DRAFT, null);
		User admin = createUser("event_admin", UserRoleType.ADMIN);

		mockMvc.perform(get("/api/events/{eventCode}", "EVT-PUBLIC"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/events/{eventCode}", "EVT-DRAFT"))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/events/{eventCode}", "EVT-DRAFT")
					.header(HttpHeaders.AUTHORIZATION, bearer(admin)))
				.andExpect(status().isOk());
	}

	@Test
	void shouldFilterUnpublishedEventsFromPublicArtistSearch() throws Exception {
		Artist artist = artistRepository.saveAndFlush(new Artist("Security Artist", "Colombia", "Pop", true));
		createEvent("EVT-ART-PUBLIC", EventStatus.PUBLISHED, artist);
		createEvent("EVT-ART-DRAFT", EventStatus.DRAFT, artist);
		User staff = createUser("artist_staff", UserRoleType.STAFF);

		mockMvc.perform(get("/api/events/by-artist").param("stageName", artist.getStageName()))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("EVT-ART-PUBLIC")))
				.andExpect(content().string(not(containsString("EVT-ART-DRAFT"))));

		mockMvc.perform(get("/api/events/by-artist")
					.param("stageName", artist.getStageName())
					.header(HttpHeaders.AUTHORIZATION, bearer(staff)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("EVT-ART-PUBLIC")))
				.andExpect(content().string(containsString("EVT-ART-DRAFT")));
	}

	private User createUser(String username, UserRoleType role) {
		User user = userRepository.saveAndFlush(new User(username, username + "@pulse.com", true));
		userCredentialRepository.saveAndFlush(new UserCredential(user, "test-only-hash"));
		userRoleRepository.saveAndFlush(new UserRole(user, role));
		return user;
	}

	private Ticket createTicket(String ticketCode, User owner) {
		Event event = createEvent("EVT-" + ticketCode, EventStatus.PUBLISHED, null);
		Ticket ticket = new Ticket();
		ticket.setTicketCode(ticketCode);
		ticket.setType(TicketType.GENERAL);
		ticket.setPrice(new BigDecimal("45.00"));
		ticket.setStatus(TicketStatus.PAID);
		ticket.setPurchaseDate(LocalDateTime.now());
		ticket.setUser(owner);
		ticket.setEvent(event);
		return ticketRepository.saveAndFlush(ticket);
	}

	private Event createEvent(String eventCode, EventStatus status, Artist artist) {
		Venue venue = venueRepository.saveAndFlush(new Venue(
				"VEN-" + eventCode, "Test Venue", "Bogota", "Test Address", 100, true));
		Event event = new Event();
		event.setEventCode(eventCode);
		event.setName(eventCode);
		event.setCategory(EventCategory.MUSIC);
		event.setStatus(status);
		event.setEventDate(LocalDateTime.now().plusDays(30));
		event.setMinimumAge(0);
		event.setVenue(venue);
		if (artist != null) {
			event.getArtists().add(artist);
		}
		return eventRepository.saveAndFlush(event);
	}

	private String bearer(User user) {
		return "Bearer " + jwtService.createAccessToken(user.getId());
	}
}