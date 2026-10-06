package com.pulse.pass.dto.request;

import com.pulse.pass.domain.TicketType;

public record PurchaseTicketRequest(
        String userEmail,
        String eventCode,
        TicketType type) {
}