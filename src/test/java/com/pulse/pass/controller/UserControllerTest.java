package com.pulse.pass.controller;

import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.service.UserService;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private UserService userService;

    @Test
    @DisplayName("POST /api/users should register a user")
    void shouldRegisterUser() throws Exception {
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

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"johndoe\",\"email\":\"john@pulse.com\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"birthDate\":\"1995-08-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService).register(any(RegisterUserRequest.class));
    }

        @Test
        @DisplayName("POST /api/users/register should not expose the legacy route")
    void shouldNotExposeLegacyRegistrationRoute() throws Exception {
        mockMvc.perform(post("/api/users/register"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404));

        verify(userService, never()).register(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("GET /api/users/by-username should return a user")
    void shouldReturnUserByUsername() throws Exception {
        given(userService.findByUsername("johndoe")).willReturn(
                new UserResponse(1L, "johndoe", "john@pulse.com", "John", "Doe", "3001234567", "Santa Marta", LocalDate.of(1995, 8, 15), true)
        );

        mockMvc.perform(get("/api/users/by-username").param("username", "johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("johndoe"));

        verify(userService).findByUsername("johndoe");
    }

    @Test
        @DisplayName("GET /api/users/by-email should return a user")
        void shouldReturnUserByEmail() throws Exception {
        given(userService.findByEmail("john@pulse.com")).willReturn(
                new UserResponse(1L, "johndoe", "john@pulse.com", "John", "Doe", "3001234567", "Santa Marta", LocalDate.of(1995, 8, 15), true)
        );

        mockMvc.perform(get("/api/users/by-email").param("email", "john@pulse.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john@pulse.com"));

        verify(userService).findByEmail("john@pulse.com");
    }

    @Test
    @DisplayName("GET /api/users/by-email without param should return 400")
    void shouldReturnBadRequestWhenEmailParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/by-email"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.email").value("Required request parameter is missing"));
    }

    @Test
    @DisplayName("GET /api/users/by-email should return 404 when the user does not exist")
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        given(userService.findByEmail("missing@pulse.com"))
                .willThrow(new ResourceNotFoundException("User not found: missing@pulse.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "missing@pulse.com"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: missing@pulse.com"));

        verify(userService).findByEmail("missing@pulse.com");
    }

    @Test
    @DisplayName("POST /api/users should return 409 for a duplicate user")
    void shouldReturnConflictWhenUserAlreadyExists() throws Exception {
        given(userService.register(any(RegisterUserRequest.class)))
                .willThrow(new DuplicateResourceException("Email already exists: john@pulse.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"johndoe\",\"email\":\"john@pulse.com\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"phone\":\"3001234567\",\"city\":\"Santa Marta\",\"birthDate\":\"1995-08-15\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists: john@pulse.com"));

        verify(userService).register(any(RegisterUserRequest.class));
    }

    @Test
    @DisplayName("POST /api/users should return validation details without calling the service")
    void shouldReturnValidationDetailsAndSkipRegistrationWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"email\":\"invalid\",\"firstName\":\"\",\"lastName\":\"Doe\",\"phone\":\"3001234567\",\"city\":\"Santa Marta\",\"birthDate\":\"1995-08-15\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.username").value("username is required"))
                .andExpect(jsonPath("$.details.email").value("email must be valid"))
                .andExpect(jsonPath("$.details.firstName").value("firstName is required"));

        verify(userService, never()).register(any(RegisterUserRequest.class));
    }

        @Test
        @DisplayName("POST /api/users should delegate future birth dates to the service")
        void shouldDelegateFutureBirthDateToService() throws Exception {
                given(userService.register(any(RegisterUserRequest.class)))
                                .willThrow(new BusinessRuleException("Birth date cannot be in the future."));

                mockMvc.perform(post("/api/users")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"username\":\"johndoe\",\"email\":\"john@pulse.com\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"birthDate\":\"" + LocalDate.now().plusDays(1) + "\"}"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Birth date cannot be in the future."));

                verify(userService).register(any(RegisterUserRequest.class));
        }
}
