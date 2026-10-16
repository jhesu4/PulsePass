package com.pulse.pass.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;

public class JwtService {

	private static final int MINIMUM_SECRET_BYTES = 32;
	private static final String EXPECTED_ALGORITHM = Jwts.SIG.HS256.getId();
	private static final String EXPECTED_TYPE = "JWT";

	private final JwtProperties properties;
	private final Clock clock;
	private final SecretKey signingKey;

	public JwtService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		validateProperties(properties);
		this.signingKey = createSigningKey(properties.getSecretBase64());
	}

	public String createAccessToken(Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("A persisted user ID is required to issue an access token");
		}

		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.getAccessTokenTtl());
		return Jwts.builder()
				.header().type("JWT").and()
				.subject(userId.toString())
				.issuer(properties.getIssuer())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.id(UUID.randomUUID().toString())
				.signWith(signingKey, Jwts.SIG.HS256)
				.compact();
	}

	public Claims validateAccessToken(String token) {
		if (token == null || token.isBlank()) {
			throw invalidToken();
		}

		try {
			Jws<Claims> signedClaims = Jwts.parser()
					.verifyWith(signingKey)
					.requireIssuer(properties.getIssuer())
					.clock(() -> Date.from(clock.instant()))
					.build()
					.parseSignedClaims(token);

			Claims claims = signedClaims.getPayload();
			if (!EXPECTED_ALGORITHM.equals(signedClaims.getHeader().getAlgorithm())
					|| !EXPECTED_TYPE.equals(signedClaims.getHeader().getType())
					|| claims.getSubject() == null || claims.getSubject().isBlank()
					|| claims.getIssuedAt() == null
					|| claims.getIssuedAt().toInstant().isAfter(clock.instant())
					|| claims.getExpiration() == null
					|| !claims.getExpiration().toInstant().isAfter(clock.instant())
					|| claims.getId() == null || claims.getId().isBlank()) {
				throw invalidToken();
			}
			return claims;
		} catch (JwtException | IllegalArgumentException exception) {
			throw invalidToken();
		}
	}

	private BadCredentialsException invalidToken() {
		return new BadCredentialsException("Invalid access token");
	}

	public long getAccessTokenTtlSeconds() {
		return properties.getAccessTokenTtl().toSeconds();
	}

	private static void validateProperties(JwtProperties properties) {
		if (properties == null || properties.getIssuer() == null || properties.getIssuer().isBlank()) {
			throw new IllegalStateException("JWT issuer must be configured");
		}
		if (properties.getAccessTokenTtl() == null || properties.getAccessTokenTtl().isZero()
				|| properties.getAccessTokenTtl().isNegative()) {
			throw new IllegalStateException("JWT access token TTL must be a positive duration");
		}
		if (properties.getSecretBase64() == null || properties.getSecretBase64().isBlank()) {
			throw new IllegalStateException("JWT secret must be configured using PULSEPASS_JWT_SECRET_BASE64");
		}
	}

	private static SecretKey createSigningKey(String secretBase64) {
		final byte[] keyBytes;
		try {
			keyBytes = Decoders.BASE64.decode(secretBase64);
		} catch (RuntimeException exception) {
			throw new IllegalStateException("JWT secret must be valid Base64", exception);
		}
		if (keyBytes.length < MINIMUM_SECRET_BYTES) {
			throw new IllegalStateException("JWT secret must decode to at least 32 random bytes");
		}
		return Keys.hmacShaKeyFor(keyBytes);
	}
}