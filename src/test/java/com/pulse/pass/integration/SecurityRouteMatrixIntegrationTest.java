package com.pulse.pass.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import com.pulse.pass.TestcontainersConfiguration;
import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserCredential;
import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleType;
import com.pulse.pass.repository.UserCredentialRepository;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import com.pulse.pass.security.JwtService;
import com.pulse.pass.service.ArtistService;
import com.pulse.pass.service.AuthService;
import com.pulse.pass.service.EventService;
import com.pulse.pass.service.TicketService;
import com.pulse.pass.service.UserService;
import com.pulse.pass.service.VenueService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class SecurityRouteMatrixIntegrationTest {

	private static final String EVENT_REQUEST = "{\"eventCode\":\"EVT-1\",\"name\":\"Event\","
			+ "\"description\":\"\",\"category\":\"MUSIC\",\"eventDate\":\"2099-12-31T20:00:00\","
			+ "\"minimumAge\":0,\"venueCode\":\"VEN-1\"}";
	private static final String REGISTER_REQUEST = "{\"username\":\"matrix-new-user\","
			+ "\"email\":\"matrix-new-user@pulse.com\",\"firstName\":\"Test\","
			+ "\"lastName\":\"User\",\"birthDate\":\"1990-01-01\","
			+ "\"password\":\"UnaFraseSeguraLarga2026!\"}";
	private static final String TICKET_REQUEST = "{\"userEmail\":\"matrix-user@pulse.com\","
			+ "\"eventCode\":\"EVT-1\",\"type\":\"GENERAL\"}";

	private static final List<RouteCase> ROUTES = List.of(
			new RouteCase(HttpMethod.GET, "/api/venues/VEN-1", null, Set.of(), 200),
			new RouteCase(HttpMethod.GET, "/api/venues/active", null, Set.of(), 200),
			new RouteCase(HttpMethod.POST, "/api/events", EVENT_REQUEST, Set.of(UserRoleType.ADMIN), 201),
			new RouteCase(HttpMethod.GET, "/api/events/EVT-1", null, Set.of(), 200),
			new RouteCase(HttpMethod.GET, "/api/events/published", null, Set.of(), 200),
			new RouteCase(HttpMethod.PATCH, "/api/events/EVT-1/publish", null,
					Set.of(UserRoleType.ADMIN), 200),
			new RouteCase(HttpMethod.POST, "/api/events/EVT-1/artists/1", "{}",
					Set.of(UserRoleType.ADMIN), 200),
			new RouteCase(HttpMethod.GET, "/api/events/by-artist?stageName=Solar%20Beat", null, Set.of(), 200),
			new RouteCase(HttpMethod.GET, "/api/artists/1", null, Set.of(), 200),
			new RouteCase(HttpMethod.GET, "/api/artists/by-stage-name?stageName=Solar%20Beat", null, Set.of(), 200),
			new RouteCase(HttpMethod.GET, "/api/artists/active", null, Set.of(), 200),
			new RouteCase(HttpMethod.POST, "/api/users", REGISTER_REQUEST, Set.of(), 201),
			new RouteCase(HttpMethod.GET, "/api/users/by-email?email=matrix-user%40pulse.com", null, Set.of(), 200),
			new RouteCase(HttpMethod.GET, "/api/users/by-username?username=matrix-user", null, Set.of(), 200),
			new RouteCase(HttpMethod.POST, "/api/tickets", TICKET_REQUEST,
					Set.of(UserRoleType.USER), 201),
			new RouteCase(HttpMethod.GET, "/api/tickets/TCK-1", null, Set.of(), 200),
			new RouteCase(HttpMethod.GET, "/api/tickets/by-user?email=matrix-user%40pulse.com", null,
					Set.of(), 200),
			new RouteCase(HttpMethod.GET, "/api/events/EVT-1/tickets/paid", null,
					Set.of(UserRoleType.STAFF, UserRoleType.ADMIN), 200),
			new RouteCase(HttpMethod.PATCH, "/api/tickets/TCK-1/cancel", null,
					Set.of(UserRoleType.USER, UserRoleType.ADMIN), 200),
			new RouteCase(HttpMethod.PATCH, "/api/tickets/TCK-1/use", null,
					Set.of(UserRoleType.STAFF, UserRoleType.ADMIN), 200));

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserCredentialRepository userCredentialRepository;

	@Autowired
	private UserRoleRepository userRoleRepository;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private VenueService venueService;

	@MockitoBean
	private EventService eventService;

	@MockitoBean
	private ArtistService artistService;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private TicketService ticketService;

	@MockitoBean
	private AuthService authService;

	@Test
	void shouldApplyAnonymousAccessPolicyToAllTwentyRoutes() throws Exception {
		assertThat(ROUTES).hasSize(20);

		for (RouteCase route : ROUTES) {
			int actualStatus = mockMvc.perform(requestFor(route, null)).andReturn().getResponse().getStatus();
			int expectedStatus = isPublicRoute(route) ? route.allowedStatus() : 401;
			assertThat(actualStatus)
					.as("Anonymous %s %s", route.method(), route.path())
					.isEqualTo(expectedStatus);
		}
	}

	@Test
	void shouldApplyRolePolicyToAllTwentyRoutes() throws Exception {
		User user = createUser("matrix-user", UserRoleType.USER);
		User staff = createUser("matrix-staff", UserRoleType.STAFF);
		User admin = createUser("matrix-admin", UserRoleType.ADMIN);

		for (RouteCase route : ROUTES) {
			for (UserRoleType role : UserRoleType.values()) {
				User actor = switch (role) {
					case USER -> user;
					case STAFF -> staff;
					case ADMIN -> admin;
				};
				String token = jwtService.createAccessToken(actor.getId());
				int actualStatus = mockMvc.perform(requestFor(route, token)).andReturn().getResponse().getStatus();
				boolean allowed = route.allowedRoles().isEmpty() || route.allowedRoles().contains(role);
				int expectedStatus = allowed ? route.allowedStatus() : 403;
				assertThat(actualStatus)
						.as("%s %s on %s %s", role, allowed ? "allowed" : "denied", route.method(), route.path())
						.isEqualTo(expectedStatus);
			}
		}
	}

	private MockHttpServletRequestBuilder requestFor(RouteCase route, String token) {
		MockHttpServletRequestBuilder request = request(route.method(), route.path());
		if (route.body() != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(route.body());
		}
		if (token != null) {
			request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		}
		return request;
	}

	private boolean isPublicRoute(RouteCase route) {
		if (route.method() == HttpMethod.POST && route.path().equals("/api/users")) {
			return true;
		}
		return route.method() == HttpMethod.GET
				&& (route.path().startsWith("/api/venues/")
						|| route.path().startsWith("/api/artists/")
						|| (route.path().startsWith("/api/events/")
								&& !route.path().contains("/tickets/")));
	}

	private User createUser(String username, UserRoleType role) {
		User user = userRepository.saveAndFlush(new User(username, username + "@pulse.com", true));
		userCredentialRepository.saveAndFlush(new UserCredential(user, "matrix-test-hash"));
		userRoleRepository.saveAndFlush(new UserRole(user, role));
		return user;
	}

	private record RouteCase(HttpMethod method, String path, String body, Set<UserRoleType> allowedRoles,
			int allowedStatus) {
	}
}