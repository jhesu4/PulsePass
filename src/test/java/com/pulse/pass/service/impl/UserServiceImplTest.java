package com.pulse.pass.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulse.pass.domain.User;
import com.pulse.pass.domain.UserProfile;
import com.pulse.pass.dto.request.RegisterUserRequest;
import com.pulse.pass.dto.response.UserResponse;
import com.pulse.pass.exception.BusinessRuleException;
import com.pulse.pass.exception.DuplicateResourceException;
import com.pulse.pass.exception.ResourceNotFoundException;
import com.pulse.pass.mapper.UserMapper;
import com.pulse.pass.repository.UserProfileRepository;
import com.pulse.pass.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, userProfileRepository, userMapper);
    }

    @Test
    void shouldRegisterUserAndProfileAsActive() {
        RegisterUserRequest request = validRequest();
        UserResponse response = response();
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0, User.class));
        when(userMapper.toResponse(any(User.class))).thenReturn(response);

        assertThat(userService.register(request)).isSameAs(response);

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(user ->
                user.isActive()
                        && user.getProfile() != null
                        && user.getProfile().getUser() == user));
        verify(userProfileRepository).save(org.mockito.ArgumentMatchers.argThat(profile ->
                profile.getUser() != null
                        && profile.getFirstName().equals(request.firstName())
                        && profile.getBirthDate().equals(request.birthDate())));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        RegisterUserRequest request = validRequest();
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already exists: andrea");

        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {
        RegisterUserRequest request = validRequest();
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists: andrea@email.com");

        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void shouldRejectFutureBirthDate() {
        RegisterUserRequest request = new RegisterUserRequest(
                "andrea", "andrea@email.com", "Andrea", "Lopez", "3001234567",
                "Santa Marta", LocalDate.now().plusDays(1));
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(request.email())).thenReturn(false);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Birth date cannot be in the future.");

        verify(userRepository, never()).save(any(User.class));
        verify(userProfileRepository, never()).save(any(UserProfile.class));
    }

    @Test
    void shouldFindUserByEmailIgnoringCase() {
        User user = new User("andrea", "andrea@email.com", true);
        UserResponse response = response();
        when(userRepository.findByEmailIgnoreCase("ANDREA@email.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        assertThat(userService.findByEmail("ANDREA@email.com")).isSameAs(response);
    }

    @Test
    void shouldThrowWhenEmailDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("missing@email.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: missing@email.com");
    }

    @Test
    void shouldFindUserByUsername() {
        User user = new User("andrea", "andrea@email.com", true);
        UserResponse response = response();
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        assertThat(userService.findByUsername("andrea")).isSameAs(response);
    }

    @Test
    void shouldThrowWhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByUsername("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: missing");
    }

    private RegisterUserRequest validRequest() {
        return new RegisterUserRequest(
                "andrea", "andrea@email.com", "Andrea", "Lopez", "3001234567",
                "Santa Marta", LocalDate.of(2001, 5, 10));
    }

    private UserResponse response() {
        return new UserResponse(
                1L, "andrea", "andrea@email.com", "Andrea", "Lopez",
                "3001234567", "Santa Marta", LocalDate.of(2001, 5, 10), true);
    }
}