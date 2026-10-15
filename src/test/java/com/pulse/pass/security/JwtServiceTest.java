package com.pulse.pass.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;

class JwtServiceTest {

	private static final String SECRET = Base64.getEncoder().encodeToString(
			"PulsePass-test-only-secret-at-least-48-bytes-1234".getBytes(StandardCharsets.UTF_8));
	private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Test
	void shouldIssueHs256JwtWithRequiredClaimsAndConfiguredExpiry() {
		JwtService jwtService = new JwtService(properties("pulsepass-api", SECRET, Duration.ofMinutes(15)), CLOCK);
		String token = jwtService.createAccessToken(42L);
		var parsedToken = Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
				.requireIssuer("pulsepass-api")
			.clock(() -> java.util.Date.from(NOW))
				.build()
				.parseSignedClaims(token);
		assertThat(parsedToken.getHeader().getAlgorithm()).isEqualTo("HS256");
		assertThat(parsedToken.getPayload().getSubject()).isEqualTo("42");
		assertThat(parsedToken.getPayload().getIssuedAt().toInstant()).isEqualTo(NOW);
		assertThat(parsedToken.getPayload().getExpiration().toInstant()).isEqualTo(NOW.plusSeconds(900));
		assertThat(parsedToken.getPayload().getId()).isNotBlank();

		Claims claims = jwtService.validateAccessToken(token);
		assertThat(claims.getSubject()).isEqualTo("42");
		assertThat(claims.getIssuer()).isEqualTo("pulsepass-api");
		assertThat(claims.getIssuedAt().toInstant()).isEqualTo(NOW);
		assertThat(claims.getExpiration().toInstant()).isEqualTo(NOW.plusSeconds(900));
		assertThat(claims.getId()).isNotBlank();
		assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(900);
	}

	@Test
	void shouldRejectExpiredTamperedAndMalformedTokens() {
		JwtService expiredTokenService = new JwtService(
				properties("pulsepass-api", SECRET, Duration.ofMinutes(15)),
				Clock.fixed(NOW.minusSeconds(1800), ZoneOffset.UTC));
		JwtService validator = new JwtService(properties("pulsepass-api", SECRET, Duration.ofMinutes(15)), CLOCK);
		String expiredToken = expiredTokenService.createAccessToken(42L);
		String validToken = validator.createAccessToken(42L);
		int signatureStart = validToken.lastIndexOf('.') + 1;
		char firstSignatureCharacter = validToken.charAt(signatureStart);
		String tamperedToken = validToken.substring(0, signatureStart)
				+ (firstSignatureCharacter == 'a' ? 'b' : 'a')
				+ validToken.substring(signatureStart + 1);

		assertThatThrownBy(() -> validator.validateAccessToken(expiredToken)).isInstanceOf(BadCredentialsException.class);
		assertThatThrownBy(() -> validator.validateAccessToken(tamperedToken)).isInstanceOf(BadCredentialsException.class);
		assertThatThrownBy(() -> validator.validateAccessToken("not-a-jwt")).isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void shouldRejectWrongIssuerAndUnexpectedAlgorithm() {
		JwtService validator = new JwtService(properties("pulsepass-api", SECRET, Duration.ofMinutes(15)), CLOCK);
		JwtService otherIssuerService = new JwtService(properties("other-api", SECRET, Duration.ofMinutes(15)), CLOCK);
		String wrongIssuerToken = otherIssuerService.createAccessToken(42L);
		byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(SECRET);
		String hs384Token = Jwts.builder()
				.header().type("JWT").and()
				.subject("42")
				.issuer("pulsepass-api")
				.issuedAt(java.util.Date.from(NOW))
				.expiration(java.util.Date.from(NOW.plusSeconds(900)))
				.id("token-id")
				.signWith(Keys.hmacShaKeyFor(keyBytes), Jwts.SIG.HS384)
				.compact();

		assertThatThrownBy(() -> validator.validateAccessToken(wrongIssuerToken))
				.isInstanceOf(BadCredentialsException.class);
		assertThatThrownBy(() -> validator.validateAccessToken(hs384Token))
				.isInstanceOf(BadCredentialsException.class);
	}

	@Test
	void shouldFailFastForMissingOrWeakSecretAndInvalidTtl() {
		assertThatThrownBy(() -> new JwtService(properties("pulsepass-api", "", Duration.ofMinutes(15)), CLOCK))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtService(properties("pulsepass-api", "c2hvcnQ=", Duration.ofMinutes(15)), CLOCK))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> new JwtService(properties("pulsepass-api", SECRET, Duration.ZERO), CLOCK))
				.isInstanceOf(IllegalStateException.class);
	}

	private JwtProperties properties(String issuer, String secret, Duration ttl) {
		JwtProperties properties = new JwtProperties();
		properties.setIssuer(issuer);
		properties.setSecretBase64(secret);
		properties.setAccessTokenTtl(ttl);
		return properties;
	}
}