package com.pulse.pass.dto.request;

import com.pulse.pass.domain.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CreateEventRequest(
        @NotBlank(message = "eventCode is required")
        String eventCode,

        @NotBlank(message = "name is required")
        String name,

        @NotBlank(message = "description is required")
        @Size(max = 500, message = "description must be at most 500 characters")
        String description,

        @NotNull(message = "category is required")
        EventCategory category,

        @NotNull(message = "eventDate is required")
        @Future(message = "eventDate must be in the future")
        LocalDateTime eventDate,

        @NotNull(message = "minimumAge is required")
        @Min(value = 0, message = "minimumAge must be greater than or equal to 0")
        Integer minimumAge,

        @NotBlank(message = "venueCode is required")
        String venueCode) {
}