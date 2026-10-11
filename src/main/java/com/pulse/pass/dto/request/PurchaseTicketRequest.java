package com.pulse.pass.dto.request;

import com.pulse.pass.domain.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PurchaseTicketRequest(
        @NotBlank(message = "userEmail is required")
        @Email(message = "userEmail must be valid")
        String userEmail,

        @NotBlank(message = "eventCode is required")
        String eventCode,

        @NotNull(message = "type is required")
        TicketType type) {
}