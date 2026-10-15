package com.pulse.pass.security;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
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
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
			CustomUserDetailsService userDetailsService, RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler)
			throws Exception {
		JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtService, userDetailsService,
				authenticationEntryPoint);
		http
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
						.requestMatchers(HttpMethod.POST, "/api/tickets").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.PATCH, "/api/tickets/*/cancel").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/users/by-email", "/api/users/by-username")
							.authenticated()
						.anyRequest().authenticated())
				.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}