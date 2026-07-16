package com.motomutterers.boardgames.auth.services;

import com.motomutterers.boardgames.auth.dto.AuthResponse;
import com.motomutterers.boardgames.auth.dto.LoginRequest;
import com.motomutterers.boardgames.auth.dto.RegisterRequest;
import com.motomutterers.boardgames.auth.exceptions.RefreshTokenExpiredException;
import com.motomutterers.boardgames.auth.exceptions.VerificationTokenExpiredException;
import com.motomutterers.boardgames.auth.exceptions.VerificationTokenNotFoundException;
import com.motomutterers.boardgames.auth.models.RefreshToken;
import com.motomutterers.boardgames.auth.models.VerificationToken;
import com.motomutterers.boardgames.auth.repositories.RefreshTokenRepository;
import com.motomutterers.boardgames.auth.repositories.VerificationTokenRepository;
import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.exceptions.ValidationException;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.model.UserStatus;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private HttpServletResponse httpServletResponse;
    @Mock private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuthService authService;

    private static final long REFRESH_EXPIRATION = 604800;      // 7 days
    private static final long VERIFICATION_EXPIRATION = 86400;   // 1 day

    @BeforeEach
    void setUp() {
        // @Value fields aren't populated in a plain Mockito test.
        ReflectionTestUtils.setField(authService, "refreshExpiration", REFRESH_EXPIRATION);
        ReflectionTestUtils.setField(authService, "verificationExpiration", VERIFICATION_EXPIRATION);
        ReflectionTestUtils.setField(authService, "baseUrl", "http://localhost:8080/");
    }

    // helpers
    private User activeUser() {
        User user = new User("test@test.com", "testuser", "hashedpassword");
        user.setIsActive();
        return user;
    }

    // --- register ---

    @Test
    void register_emailAlreadyTaken_throwsValidationException() {
        RegisterRequest request = new RegisterRequest("newuser", "taken@test.com", "password1");

        when(userRepository.findByEmail("taken@test.com")).thenReturn(Optional.of(new User()));
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_usernameAlreadyTaken_throwsValidationException() {
        RegisterRequest request = new RegisterRequest("takenuser", "new@test.com", "password1");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("takenuser")).thenReturn(Optional.of(new User()));

        assertThrows(ValidationException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_validRequest_savesUserTokenAndSendsEmail() {
        RegisterRequest request = new RegisterRequest("newuser", "new@test.com", "password1");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password1")).thenReturn("hashed");

        authService.register(request);

        // The user is persisted with the hashed password.
        verify(userRepository).save(argThat(u ->
            "new@test.com".equals(u.getEmail())
                && "newuser".equals(u.getUsername())
                && "hashed".equals(u.getPasswordHash())));
        // A verification token is created and a verification email is dispatched.
        verify(verificationTokenRepository).save(any(VerificationToken.class));
        verify(emailService).sendEmail(eq("new@test.com"), anyString(), anyString());
    }

    // --- login ---

    @Test
    void login_userNotFound_throwsValidationException() {
        LoginRequest request = new LoginRequest(false, "unknown@test.com", "password1");

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> authService.login(request, httpServletResponse));
    }

    @Test
    void login_byUsername_looksUpByUsername() {
        LoginRequest request = new LoginRequest(true, "testuser", "password1");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> authService.login(request, httpServletResponse));
        verify(userRepository).findByUsername("testuser");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_unverifiedUser_throwsValidationException() {
        LoginRequest request = new LoginRequest(false, "test@test.com", "password1");

        // A freshly-constructed user defaults to PENDING_VERIFICATION.
        User user = new User("test@test.com", "testuser", "hashedpassword");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        assertThrows(ValidationException.class, () -> authService.login(request, httpServletResponse));
    }

    @Test
    void login_wrongPassword_throwsValidationException() {
        LoginRequest request = new LoginRequest(false, "test@test.com", "wrongpassword");

        User user = activeUser();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashedpassword")).thenReturn(false);

        assertThrows(ValidationException.class, () -> authService.login(request, httpServletResponse));
    }

    @Test
    void login_validCredentials_returnsTokenAndSetsRefreshCookie() {
        LoginRequest request = new LoginRequest(false, "test@test.com", "password1");

        User user = activeUser();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password1", "hashedpassword")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(request, httpServletResponse);

        assertEquals("jwt-token", response.getAccessToken());
        // Refresh token persisted and set as an httpOnly cookie.
        verify(refreshTokenRepository).save(any(RefreshToken.class));
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(httpServletResponse).addCookie(cookieCaptor.capture());
        Cookie cookie = cookieCaptor.getValue();
        assertEquals("refreshToken", cookie.getName());
        assertTrue(cookie.isHttpOnly());
    }

    // --- logout ---

    @Test
    void logout_validToken_deletesTokenAndClearsCookie() {
        User user = activeUser();
        RefreshToken token = new RefreshToken(user, "refresh-value", LocalDateTime.now().plusDays(1));

        when(httpServletRequest.getCookies())
            .thenReturn(new Cookie[]{ new Cookie("refreshToken", "refresh-value") });
        when(refreshTokenRepository.findByToken("refresh-value")).thenReturn(Optional.of(token));

        authService.logout(httpServletRequest, httpServletResponse);

        verify(refreshTokenRepository).delete(token);
        // A cleared cookie (maxAge 0) is written back.
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(httpServletResponse).addCookie(cookieCaptor.capture());
        assertEquals(0, cookieCaptor.getValue().getMaxAge());
    }

    @Test
    void logout_tokenNotFound_throwsRefreshTokenExpired() {
        when(httpServletRequest.getCookies())
            .thenReturn(new Cookie[]{ new Cookie("refreshToken", "missing") });
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThrows(RefreshTokenExpiredException.class,
            () -> authService.logout(httpServletRequest, httpServletResponse));
    }

    // --- refresh ---

    @Test
    void refresh_validToken_rotatesExpiryAndReturnsNewAccessToken() {
        User user = activeUser();
        RefreshToken token = new RefreshToken(user, "refresh-value", LocalDateTime.now().plusDays(1));

        when(httpServletRequest.getCookies())
            .thenReturn(new Cookie[]{ new Cookie("refreshToken", "refresh-value") });
        when(refreshTokenRepository.findByToken("refresh-value")).thenReturn(Optional.of(token));
        when(jwtService.generateToken(user)).thenReturn("new-jwt");

        AuthResponse response = authService.refresh(httpServletRequest, httpServletResponse);

        assertEquals("new-jwt", response.getAccessToken());
        // The refresh token's expiry is pushed forward and re-saved.
        assertTrue(token.getExpiresAt().isAfter(LocalDateTime.now().plusSeconds(REFRESH_EXPIRATION - 60)));
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void refresh_expiredToken_throwsRefreshTokenExpired() {
        User user = activeUser();
        RefreshToken token = new RefreshToken(user, "refresh-value", LocalDateTime.now().minusDays(1));

        when(httpServletRequest.getCookies())
            .thenReturn(new Cookie[]{ new Cookie("refreshToken", "refresh-value") });
        when(refreshTokenRepository.findByToken("refresh-value")).thenReturn(Optional.of(token));

        assertThrows(RefreshTokenExpiredException.class,
            () -> authService.refresh(httpServletRequest, httpServletResponse));
    }

    // --- verifyEmail ---

    @Test
    void verifyEmail_validToken_activatesUser() {
        User user = new User("test@test.com", "testuser", "hashedpassword");
        VerificationToken token = new VerificationToken(user, "verify", LocalDateTime.now().plusHours(1));

        when(verificationTokenRepository.findByToken("verify")).thenReturn(Optional.of(token));

        authService.verifyEmail("verify");

        assertEquals(UserStatus.ACTIVE, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_tokenNotFound_throws() {
        when(verificationTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThrows(VerificationTokenNotFoundException.class, () -> authService.verifyEmail("nope"));
    }

    @Test
    void verifyEmail_expiredToken_throws() {
        User user = new User("test@test.com", "testuser", "hashedpassword");
        VerificationToken token = new VerificationToken(user, "verify", LocalDateTime.now().minusHours(1));

        when(verificationTokenRepository.findByToken("verify")).thenReturn(Optional.of(token));

        assertThrows(VerificationTokenExpiredException.class, () -> authService.verifyEmail("verify"));
        verify(userRepository, never()).save(any());
    }
}
