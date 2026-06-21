package com.motomutterers.boardgames.auth.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.auth.RefreshTokenRepository;
import com.motomutterers.boardgames.auth.dto.AuthResponse;
import com.motomutterers.boardgames.auth.dto.LoginRequest;
import com.motomutterers.boardgames.auth.dto.RegisterRequest;
import com.motomutterers.boardgames.auth.exceptions.PasswordIncorrectException;
import com.motomutterers.boardgames.auth.models.RefreshToken;
import com.motomutterers.boardgames.exceptions.ValidationBuilder;
import com.motomutterers.boardgames.user.UserRepository;
import com.motomutterers.boardgames.user.exceptions.UserNotFoundException;
import com.motomutterers.boardgames.user.model.User;
import com.motomutterers.boardgames.user.services.UserService;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtService jwtService;    
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public AuthService(
        UserRepository userRepository,
        UserService userService,
        JwtService jwtService,
        RefreshTokenRepository refreshTokenRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest request){
        new ValidationBuilder()
            .addError(userRepository.findByEmail(request.getEmail()).isPresent(), "email", "Email is already taken")
            .addError(userRepository.findByUsername(request.getUsername()).isPresent(), "username", "Username is already taken")
            .validate();

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = new User(request.getEmail(), request.getUsername(), passwordHash);
        userRepository.save(user);
        //EMAIL SERVICE FOR SENDING VERIFICATION EMAIL
    }

    public AuthResponse login(LoginRequest request){
        Optional<User> result;
        String primaryKey = request.getPrimaryKey();
        if(request.getIsUsername()){
            result = userRepository.findByUsername(primaryKey);
        } else {
            result = userRepository.findByEmail(primaryKey);
        }

        if(result.isEmpty()){
            throw new UserNotFoundException("User was not found");
        }

        User user = result.get();

        if(!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())){
            throw new PasswordIncorrectException("Password is incorrect, try again.");
        }
                
        String accessToken = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();


        RefreshToken token = new RefreshToken(user, refreshToken, LocalDateTime.now().plusSeconds(refreshExpiration/1000));
        refreshTokenRepository.save(token);
        AuthResponse response = new AuthResponse(accessToken, refreshToken);

        return response;
    }

    public void logout(UUID userId){
        User user = userService.getUserById(userId);
        if(refreshTokenRepository.findByUser(user).isEmpty()){
            throw new UserNotFoundException("User is not logged in");
        }
        refreshTokenRepository.deleteByUser(user);
    }
}
