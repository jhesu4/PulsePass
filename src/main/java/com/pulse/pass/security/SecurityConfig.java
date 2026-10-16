package com.pulse.pass.security;

import com.pulse.pass.domain.UserRole;
import com.pulse.pass.domain.UserRoleType;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.repository.UserRepository;
import com.pulse.pass.repository.UserRoleRepository;
import java.time.Clock;
import java.util.List;
import java.util.Locale;

import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	Clock jwtClock() {
		return Clock.systemUTC();
	}

	@Bean
	JwtService jwtService(JwtProperties properties, Clock jwtClock) {
		return new JwtService(properties, jwtClock);
	}

	@Bean
	@Profile("local")
	@ConditionalOnProperty(prefix = "pulsepass.security.bootstrap", name = "enabled", havingValue = "true")
	ApplicationRunner localRoleBootstrapRunner(Environment environment, UserRepository userRepository,
			UserRoleRepository userRoleRepository) {
		return args -> {
			String email = environment.getRequiredProperty("pulsepass.security.bootstrap.email");
			String roleName = environment.getRequiredProperty("pulsepass.security.bootstrap.role");
			UserRoleType role = UserRoleType.valueOf(roleName.trim().toUpperCase(Locale.ROOT));
			if (role != UserRoleType.STAFF && role != UserRoleType.ADMIN) {
				throw new IllegalArgumentException("Local role bootstrap only supports STAFF or ADMIN");
			}

			var user = userRepository.findByEmailIgnoreCase(email)
					.orElseThrow(() -> new ResourceNotFoundException("Bootstrap user not found"));
			boolean alreadyAssigned = userRoleRepository.findAllByUser_Id(user.getId()).stream()
					.anyMatch(userRole -> userRole.getRole() == role);
			if (!alreadyAssigned) {
				userRoleRepository.save(new UserRole(user, role));
			}
		};
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(properties.getAllowedOrigins());
		cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type", RequestIdFilter.HEADER_NAME,
				"X-Admin-Purpose"));
		cors.setExposedHeaders(List.of(RequestIdFilter.HEADER_NAME));
		cors.setAllowCredentials(false);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", cors);
		return source;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			CustomUserDetailsService userDetailsService, RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler, SecurityAuditLogger securityAuditLogger,
			CorsConfigurationSource corsConfigurationSource, ObjectMapper objectMapper,
			@Value("${pulsepass.security.transport.require-https:true}") boolean requireHttps)
			throws Exception {
		JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService,
				authenticationEntryPoint);
		RequestIdFilter requestIdFilter = new RequestIdFilter(objectMapper, securityAuditLogger, requireHttps);
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(formLogin -> formLogin.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/users").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/venues/**", "/api/artists/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/events/published", "/api/events/by-artist", "/api/events/*")
							.permitAll()
						.requestMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/events/*/publish").hasRole("ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/events/*/artists/*").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/events/*/tickets/paid")
							.hasAnyRole("STAFF", "ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/tickets/*/use")
							.hasAnyRole("STAFF", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/tickets").hasRole("USER")
						.requestMatchers(HttpMethod.PATCH, "/api/tickets/*/cancel").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/users/by-email", "/api/users/by-username")
							.authenticated()
						.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(requestIdFilter, CorsFilter.class);

		return http.build();
	}

}