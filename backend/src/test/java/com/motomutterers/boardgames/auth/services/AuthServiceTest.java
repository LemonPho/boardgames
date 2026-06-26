package com.motomutterers.boardgames.auth.services;

import com.motomutterers.boardgames.auth.dto.LoginRequest;
import com.motomutterers.boardgames.auth.dto.RegisterRequest;
import com.motomutterers.boardgames.auth.exceptions.PasswordIncorrectException;
import com.motomutterers.boardgames.auth.repositories.RefreshTokenRepository;
import com.motomutterers.boardgames.auth.repositories.VerificationTokenRepository;
import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.exceptions.ValidationException;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.exceptions.UserNotFoundException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserService userService;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HttpServletResponse httpServletResponse;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_emailAlreadyTaken_throwsValidationException() {
        RegisterRequest request = new RegisterRequest("newuser", "taken@test.com", "password1");

        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> authService.register(request));
    }

    @Test
    void register_usernameAlreadyTaken_throwsValidationException() {
        RegisterRequest request = new RegisterRequest("takenuser", "new@test.com", "password1");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("takenuser")).thenReturn(Optional.of(new User()));

        assertThrows(ValidationException.class, () -> authService.register(request));
    }

    @Test
    void login_userNotFound_throwsUserNotFoundException() {
        LoginRequest request = new LoginRequest(false, "unknown@test.com", "password1");

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.login(request, httpServletResponse));
    }

    @Test
    void login_wrongPassword_throwsPasswordIncorrectException() {
        LoginRequest request = new LoginRequest(false, "test@test.com", "wrongpassword");

        User user = new User("test@test.com", "testuser", "hashedpassword");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

        assertThrows(PasswordIncorrectException.class, () -> authService.login(request, httpServletResponse));
    }

    @Test
    void login_validCredentials_returnsAuthResponse() {
        LoginRequest request = new LoginRequest(false, "test@test.com", "password1");

        User user = new User("test@test.com", "testuser", "hashedpassword");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "hashedpassword")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        var response = authService.login(request, httpServletResponse);

        assertNotNull(response.getAccessToken());
        verify(httpServletResponse).addCookie(any());
    }
}