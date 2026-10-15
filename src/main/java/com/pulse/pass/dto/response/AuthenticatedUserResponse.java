package com.pulse.pass.dto.response;

import java.util.List;

public record AuthenticatedUserResponse(Long userId, String username, List<String> roles) {
}