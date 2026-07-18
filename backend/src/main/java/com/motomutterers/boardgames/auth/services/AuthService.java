package com.motomutterers.boardgames.auth.services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.auth.dto.AuthResponse;
import com.motomutterers.boardgames.auth.dto.LoginRequest;
import com.motomutterers.boardgames.auth.dto.RegisterRequest;
import com.motomutterers.boardgames.auth.exceptions.RefreshTokenExpiredException;
import com.motomutterers.boardgames.auth.exceptions.VerificationTokenExpiredException;
import com.motomutterers.boardgames.auth.exceptions.VerificationTokenNotFoundException;
import com.motomutterers.boardgames.auth.models.RefreshToken;
import com.motomutterers.boardgames.auth.models.VerificationToken;
import com.motomutterers.boardgames.auth.models.VerificationTokenType;
import com.motomutterers.boardgames.auth.repositories.RefreshTokenRepository;
import com.motomutterers.boardgames.auth.repositories.VerificationTokenRepository;
import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.email.EmailTemplates;
import com.motomutterers.boardgames.exceptions.ValidationBuilder;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.model.User;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;    
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Value("${spring.mail.verification-expiration}")
    private long verificationExpiration;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public AuthService(
        UserRepository userRepository,
        JwtService jwtService,
        RefreshTokenRepository refreshTokenRepository,
        VerificationTokenRepository verificationTokenRepository,
        EmailService emailService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    private VerificationToken createVerificationToken(User user){
        return new VerificationToken(user, UUID.randomUUID().toString(), LocalDateTime.now().plusSeconds(verificationExpiration));
    }

    private RefreshToken getRefreshTokenByToken(String refreshTokenString){
        Optional<RefreshToken> result = refreshTokenRepository.findByToken(refreshTokenString);
        if(result.isEmpty()){
            throw new RefreshTokenExpiredException("You need to login");
        }

        RefreshToken refreshToken = result.get();

        if(refreshToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new RefreshTokenExpiredException("You need to login");
        }

        return refreshToken;
    }

    private VerificationToken getVerificationTokenByToken(String verificationToken){
        return verificationTokenRepository.findByToken(verificationToken)
            .orElseThrow(() -> new VerificationTokenNotFoundException("Verification toke not found"));
    }

    private HttpServletResponse createRefreshToken(User user, HttpServletResponse response){
        String refreshToken = UUID.randomUUID().toString();
        RefreshToken token = new RefreshToken(user, refreshToken, LocalDateTime.now().plusSeconds(refreshExpiration));
        refreshTokenRepository.save(token);

        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // change when going to https
        cookie.setPath("/");
        cookie.setMaxAge((int)(refreshExpiration));
        response.addCookie(cookie);

        return response;
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Transactional
    public void register(RegisterRequest request){
        new ValidationBuilder()
            .addError(userRepository.findByEmail(request.getEmail()).isPresent(), "email", "Email is already taken")
            .addError(userRepository.findByUsername(request.getUsername()).isPresent(), "username", "Username is already taken")
            .validate();

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), request.getUsername(), passwordHash);
        userRepository.save(user);

        VerificationToken verificationToken = createVerificationToken(user);
        verificationTokenRepository.save(verificationToken);

        String verificationLink = frontendBaseUrl + "verify?token=" + verificationToken.getToken();
        String html = EmailTemplates.verificationEmail(user.getUsername(), verificationLink);
        emailService.sendEmail(user.getEmail(), "Verify your Boardgames account", html);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response){
        Optional<User> result;
        String primaryKey = request.getPrimaryKey();
        if(request.getIsUsername()){
            result = userRepository.findByUsername(primaryKey);
        } else {
            result = userRepository.findByEmail(primaryKey);
        }

        ValidationBuilder validationBuilder = new ValidationBuilder();

        if(result.isEmpty()){
            validationBuilder.addError(true, "userExists", "User wasn't found");
            validationBuilder.validate();
        }

        User user = result.get();
        validationBuilder.addError(!user.isActive(), "isActive", "You need to verify your email before logging in");
        validationBuilder.addError(
            !passwordEncoder.matches(request.getPassword(), user.getPasswordHash()),
            "password", 
            "Password is incorrect");
        validationBuilder.validate();
                
        String accessToken = jwtService.generateToken(user);
        response = createRefreshToken(user, response);

        return new AuthResponse(accessToken);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response){
        String refreshTokenString = getCookieValue(request, "refreshToken");
        RefreshToken refreshToken = getRefreshTokenByToken(refreshTokenString);
        refreshTokenRepository.delete(refreshToken);

        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response){
        String refreshTokenString = getCookieValue(request, "refreshToken");
        RefreshToken refreshToken = getRefreshTokenByToken(refreshTokenString);
        User user = refreshToken.getUser();

        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshExpiration));
        refreshTokenRepository.save(refreshToken);
        String accessToken = jwtService.generateToken(user);

        return new AuthResponse(accessToken);
    }

    @Transactional
    public void verifyEmail(String token){
        VerificationToken verificationToken = getVerificationTokenByToken(token);
        if(verificationToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new VerificationTokenExpiredException("Verification token is expired, please resend a new verification email");
        }

        User user = verificationToken.getUser();

        // EMAIL_CHANGE tokens apply the pending address; older/registration tokens
        // (type null pre-migration or ACCOUNT_VERIFICATION) activate the account.
        if(VerificationTokenType.EMAIL_CHANGE.equals(verificationToken.getType())){
            user.setEmail(verificationToken.getPendingEmail());
        } else {
            user.setIsActive();
            user.setVerified(true);
        }

        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);
    }

    // Starts an email change: the address only changes once the new inbox confirms
    // the link, so a typo can't lock the account. Guarded by the current password.
    @Transactional
    public void requestEmailChange(User user, String newEmail, String currentPassword){
        new ValidationBuilder()
            .addError(!passwordEncoder.matches(currentPassword, user.getPasswordHash()), "currentPassword", "Password is incorrect")
            .validate();

        if(!newEmail.equals(user.getEmail())){
            new ValidationBuilder()
                .addError(userRepository.findByEmail(newEmail).isPresent(), "email", "Email is already taken")
                .validate();
        }

        VerificationToken token = new VerificationToken(
            user,
            UUID.randomUUID().toString(),
            VerificationTokenType.EMAIL_CHANGE,
            newEmail,
            LocalDateTime.now().plusSeconds(verificationExpiration));
        verificationTokenRepository.save(token);

        String verificationLink = frontendBaseUrl + "verify?token=" + token.getToken();
        String html = EmailTemplates.emailChangeEmail(user.getUsername(), verificationLink);
        // Sent to the NEW address — that inbox must prove ownership.
        emailService.sendEmail(newEmail, "Confirm your new email", html);
    }

    // Emails a password-reset link if the account exists. Silent no-op when it
    // doesn't, so this endpoint can't be used to probe which accounts are real.
    @Transactional
    public void requestPasswordReset(boolean isUsername, String primaryKey){
        Optional<User> result = isUsername
            ? userRepository.findByUsername(primaryKey)
            : userRepository.findByEmail(primaryKey);
        if(result.isEmpty()) return;

        User user = result.get();
        VerificationToken token = new VerificationToken(
            user,
            UUID.randomUUID().toString(),
            VerificationTokenType.PASSWORD_RESET,
            null,
            LocalDateTime.now().plusSeconds(verificationExpiration));
        verificationTokenRepository.save(token);

        String resetLink = frontendBaseUrl + "reset-password?token=" + token.getToken();
        String html = EmailTemplates.passwordResetEmail(user.getUsername(), resetLink);
        emailService.sendEmail(user.getEmail(), "Reset your password", html);
    }

    // Completes a reset: the token must exist, be a PASSWORD_RESET token, and be
    // unexpired. On success the password is replaced and the token consumed.
    @Transactional
    public void resetPassword(String token, String newPassword){
        VerificationToken verificationToken = getVerificationTokenByToken(token);

        if(!VerificationTokenType.PASSWORD_RESET.equals(verificationToken.getType())){
            throw new VerificationTokenNotFoundException("Invalid password reset link");
        }
        if(verificationToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new VerificationTokenExpiredException("This reset link has expired, please request a new one");
        }

        User user = verificationToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);
    }
}
