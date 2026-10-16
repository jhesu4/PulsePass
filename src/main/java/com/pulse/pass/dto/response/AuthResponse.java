package com.pulse.pass.dto.response;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {
}