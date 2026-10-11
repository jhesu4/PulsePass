package com.pulse.pass.controller;

import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.exception.GlobalExceptionHandler;
import com.pulse.pass.service.UserService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    @Test
    @DisplayName("POST /api/users/register should register a user")
    void shouldRegisterUser() throws Exception {
        UserService userService = mock(UserService.class);
        UserResponse response = new UserResponse(
                1L,
                "johndoe",
                "john@pulse.com",
                "John",
                "Doe",
                "3001234567",
                "Santa Marta",
                LocalDate.of(1995, 8, 15),
                true
        );

        given(userService.register(any(RegisterUserRequest.class))).willReturn(response);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"johndoe\",\"email\":\"john@pulse.com\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"phone\":\"3001234567\",\"city\":\"Santa Marta\",\"birthDate\":\"1995-08-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("GET /api/users/email/{email} should return a user")
    void shouldReturnUserByEmail() throws Exception {
        UserService userService = mock(UserService.class);
        given(userService.findByEmail("john@pulse.com")).willReturn(
                new UserResponse(1L, "johndoe", "john@pulse.com", "John", "Doe", "3001234567", "Santa Marta", LocalDate.of(1995, 8, 15), true)
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/users/email/john@pulse.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@pulse.com"));

        verify(userService).findByEmail("john@pulse.com");
    }

    @Test
    @DisplayName("GET /api/users/by-email should match the PRD contract")
    void shouldExposeByEmailAliasEndpoint() throws Exception {
        UserService userService = mock(UserService.class);
        given(userService.findByEmail("john@pulse.com")).willReturn(
                new UserResponse(1L, "johndoe", "john@pulse.com", "John", "Doe", "3001234567", "Santa Marta", LocalDate.of(1995, 8, 15), true)
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/users/by-email").param("email", "john@pulse.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@pulse.com"));

        verify(userService).findByEmail("john@pulse.com");
    }

    @Test
    @DisplayName("GET /api/users/by-email without param should return 400")
    void shouldReturnBadRequestWhenEmailParameterIsMissing() throws Exception {
        UserService userService = mock(UserService.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/users/by-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
