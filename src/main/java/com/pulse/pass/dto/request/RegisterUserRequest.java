package com.pulse.pass.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record RegisterUserRequest(
        @NotBlank(message = "username is required")
        @Pattern(regexp = "^[^@]*$", message = "username must not contain @")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "firstName is required")
        String firstName,

        @NotBlank(message = "lastName is required")
        String lastName,

        String phone,

        String city,

        @NotNull(message = "birthDate is required")
        LocalDate birthDate,

        @NotBlank(message = "password is required")
        @ValidPassword
        String password) {
}
