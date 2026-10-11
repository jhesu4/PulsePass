package com.pulse.pass.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record RegisterUserRequest(
        @NotBlank(message = "username is required")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "firstName is required")
        String firstName,

        @NotBlank(message = "lastName is required")
        String lastName,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "phone format is invalid")
        String phone,

        @NotBlank(message = "city is required")
        String city,

        @NotNull(message = "birthDate is required")
        @Past(message = "birthDate must be in the past")
        LocalDate birthDate) {
}
