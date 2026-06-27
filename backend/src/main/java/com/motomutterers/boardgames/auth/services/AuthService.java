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
import com.motomutterers.boardgames.auth.exceptions.PasswordIncorrectException;
import com.motomutterers.boardgames.auth.exceptions.RefreshTokenExpiredException;
import com.motomutterers.boardgames.auth.exceptions.VerificationTokenExpiredException;
import com.motomutterers.boardgames.auth.exceptions.VerificationTokenNotFoundException;
import com.motomutterers.boardgames.auth.models.RefreshToken;
import com.motomutterers.boardgames.auth.models.VerificationToken;
import com.motomutterers.boardgames.auth.repositories.RefreshTokenRepository;
import com.motomutterers.boardgames.auth.repositories.VerificationTokenRepository;
import com.motomutterers.boardgames.email.EmailService;
import com.motomutterers.boardgames.email.EmailTemplates;
import com.motomutterers.boardgames.exceptions.ValidationBuilder;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.exceptions.UserNotFoundException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
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

    public AuthService(
        UserRepository userRepository,
        UserService userService,
        JwtService jwtService,
        RefreshTokenRepository refreshTokenRepository,
        VerificationTokenRepository verificationTokenRepository,
        EmailService emailService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
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

        String verificationLink = baseUrl + "api/auth/verify?token=" + verificationToken.getToken();
        String html = EmailTemplates.verificationEmail(user.getUsername(), verificationLink);
        emailService.sendEmail(user.getEmail(), "Verify your Boardgames account", html);
    }

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

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            validationBuilder.addError(true, "password", "Password is incorrect");
            validationBuilder.validate();
        }
                
        String accessToken = jwtService.generateToken(user);
        response = createRefreshToken(user, response);

        return new AuthResponse(accessToken);
    }

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

    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response){
        String refreshTokenString = getCookieValue(request, "refreshToken");
        RefreshToken refreshToken = getRefreshTokenByToken(refreshTokenString);
        User user = refreshToken.getUser();

        refreshTokenRepository.delete(refreshToken);

        String accessToken = jwtService.generateToken(user);
        response = createRefreshToken(user, response);

        return new AuthResponse(accessToken);
    }

    public void verifyEmail(String token){
        VerificationToken verificationToken = getVerificationTokenByToken(token);
        if(verificationToken.getExpiresAt().isBefore(LocalDateTime.now())){
            throw new VerificationTokenExpiredException("Verification token is expired, please resend a new verification email");
        }

        User user = verificationToken.getUser();
        user.setIsActive();
        userRepository.save(user);
    }
}
