package com.motomutterers.boardgames.auth.controllers;

import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.auth.dto.AuthResponse;
import com.motomutterers.boardgames.auth.dto.LoginRequest;
import com.motomutterers.boardgames.auth.dto.RefreshRequest;
import com.motomutterers.boardgames.auth.dto.RegisterRequest;
import com.motomutterers.boardgames.auth.services.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(
        AuthService authService
    ) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request){
        authService.register(request);
        return ResponseEntity.ok("Check your email to verify your account");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestBody RefreshRequest request){
        authService.refresh(request);
        return ResponseEntity.ok("Token refreshed");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody RefreshRequest request){
        authService.logout(request);
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token){
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified");
    }
}
