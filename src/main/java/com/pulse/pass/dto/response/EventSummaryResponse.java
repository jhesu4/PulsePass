package com.pulse.pass.dto.response;

import com.pulse.pass.domain.EventCategory;
import com.pulse.pass.domain.EventStatus;
import java.time.LocalDateTime;

public record EventSummaryResponse(
	Long id,
	String eventCode,
	String name,
	EventCategory category,
	EventStatus status,
	LocalDateTime eventDate,
	String venueCode,
	String venueName) {
}
