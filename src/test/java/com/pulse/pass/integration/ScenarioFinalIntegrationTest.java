package com.pulse.pass.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pulse.pass.TestcontainersConfiguration;
import com.pulse.pass.domain.Artist;
import com.pulse.pass.domain.Event;
import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import com.pulse.pass.domain.TicketStatus;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleType;
import com.pulse.pass.domain.Venue;
import com.pulse.pass.dto.response.AuthResponse;
import com.pulse.pass.dto.response.TicketResponse;
import com.pulse.pass.repository.ArtistRepository;
import com.pulse.pass.repository.EventRepository;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import com.pulse.pass.repository.VenueRepository;
import com.pulse.pass.security.JwtService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ScenarioFinalIntegrationTest {

	private static final String EVENT_CODE = "CMF-2026";
	private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private VenueRepository venueRepository;

	@Autowired
	private ArtistRepository artistRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCredentialRepository userCredentialRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Test
	void shouldCompleteFinalSecurityDemonstrationScenario() throws Exception {
		Event event = createDraftEvent();
		Artist solarBeat = artistRepository.findByStageName("Solar Beat").orElseThrow();

		User andrea = register("andrea_demo", "andrea.demo@pulse.com");
		User carlos = register("carlos_demo", "carlos.demo@pulse.com");
		User laura = register("laura_demo", "laura.demo@pulse.com");
		User admin = register("admin_demo", "admin.demo@pulse.com");
		userRoleRepository.saveAndFlush(new UserRole(laura, UserRoleType.STAFF));
		userRoleRepository.saveAndFlush(new UserRole(admin, UserRoleType.ADMIN));

		UserCredential credential = userCredentialRepository.findByUser_Id(andrea.getId()).orElseThrow();
		assertThat(credential.getPasswordHash()).isNotEqualTo(PASSWORD);
		assertThat(passwordEncoder.matches(PASSWORD, credential.getPasswordHash())).isTrue();

		String andreaToken = login("andrea_demo", andrea.getEmail());
		String carlosToken = login("carlos_demo", carlos.getEmail());
		String lauraToken = login("laura_demo", laura.getEmail());
		String adminToken = login("admin_demo", admin.getEmail());

		mockMvc.perform(get("/api/events/published"))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString(EVENT_CODE))));

		mockMvc.perform(patch("/api/events/{eventCode}/publish", EVENT_CODE)
					.header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/events/{eventCode}/publish", EVENT_CODE)
					.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PUBLISHED"));

		mockMvc.perform(post("/api/events/{eventCode}/artists/{artistId}", EVENT_CODE, solarBeat.getId())
					.header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

		mockMvc.perform(get("/api/events/published"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(EVENT_CODE)));

		TicketResponse andreaTicket = purchaseTicket(andrea.getEmail(), andreaToken);
		assertThat(andreaTicket.status()).isEqualTo(TicketStatus.PAID);
		assertThat(andreaTicket.eventCode()).isEqualTo(EVENT_CODE);

		TicketResponse carlosTicket = purchaseTicket(carlos.getEmail(), carlosToken);
		mockMvc.perform(get("/api/tickets/{ticketCode}", carlosTicket.ticketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
				.andExpect(status().isForbidden());
		mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", carlosTicket.ticketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
				.andExpect(status().isForbidden());

		mockMvc.perform(patch("/api/tickets/{ticketCode}/use", carlosTicket.ticketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(lauraToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("USED"));

		TicketResponse cancelledTicket = purchaseTicket(carlos.getEmail(), carlosToken);
		mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel", cancelledTicket.ticketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(carlosToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
		mockMvc.perform(patch("/api/tickets/{ticketCode}/use", cancelledTicket.ticketCode())
					.header(HttpHeaders.AUTHORIZATION, bearer(lauraToken)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Only PAID tickets can be marked as used: " + cancelledTicket.ticketCode()));

		mockMvc.perform(get("/api/auth/me")
					.header(HttpHeaders.AUTHORIZATION, bearer(tamperSignature(andreaToken))))
				.andExpect(status().isUnauthorized());

		andrea.setActive(false);
		userRepository.flush();
		mockMvc.perform(get("/api/auth/me")
					.header(HttpHeaders.AUTHORIZATION, bearer(andreaToken)))
				.andExpect(status().isUnauthorized());

		assertThat(eventRepository.findByEventCode(EVENT_CODE).orElseThrow().getStatus())
				.isEqualTo(EventStatus.PUBLISHED);
	}

	private Event createDraftEvent() {
		Venue venue = venueRepository.saveAndFlush(new Venue(
				"VEN-SMR-01", "Sala Mar", "Santa Marta", "Carrera 1", 300, true));
		Event event = new Event();
		event.setEventCode(EVENT_CODE);
		event.setName("Festival CMF 2026");
		event.setDescription("Festival de demostracion de PulsePass");
		event.setCategory(EventCategory.MUSIC);
		event.setStatus(EventStatus.DRAFT);
		event.setEventDate(LocalDateTime.now().plusDays(45));
		event.setMinimumAge(0);
		event.setVenue(venue);
		return eventRepository.saveAndFlush(event);
	}

	private User register(String username, String email) throws Exception {
		String request = "{\"username\":\"" + username + "\",\"email\":\"" + email
				+ "\",\"firstName\":\"Demo\",\"lastName\":\"User\",\"phone\":\"3001234567\","
				+ "\"city\":\"Santa Marta\",\"birthDate\":\"1990-12-10\",\"password\":\"" + PASSWORD + "\"}";
		mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
		return userRepository.findByUsername(username).orElseThrow();
	}

	private String login(String username, String email) throws Exception {
		String request = "{\"identifier\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}";
		MvcResult result = mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(900))
				.andReturn();
		AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
		assertThat(jwtService.validateAccessToken(response.accessToken()).getSubject())
				.isEqualTo(userRepository.findByEmailIgnoreCase(email).orElseThrow().getId().toString());
		return response.accessToken();
	}

	private TicketResponse purchaseTicket(String email, String token) throws Exception {
		String request = "{\"userEmail\":\"" + email + "\",\"eventCode\":\"" + EVENT_CODE
				+ "\",\"type\":\"GENERAL\"}";
		MvcResult result = mockMvc.perform(post("/api/tickets")
					.header(HttpHeaders.AUTHORIZATION, bearer(token))
					.contentType(MediaType.APPLICATION_JSON)
					.content(request))
				.andExpect(status().isCreated())
				.andReturn();
		return objectMapper.readValue(result.getResponse().getContentAsString(), TicketResponse.class);
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private String tamperSignature(String token) {
		int signatureStart = token.lastIndexOf('.') + 1;
		char firstCharacter = token.charAt(signatureStart);
		return token.substring(0, signatureStart)
				+ (firstCharacter == 'a' ? 'b' : 'a')
				+ token.substring(signatureStart + 1);
	}
}