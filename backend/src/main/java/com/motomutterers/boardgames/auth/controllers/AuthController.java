package com.motomutterers.boardgames.auth.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.motomutterers.boardgames.auth.dto.AuthResponse;
import com.motomutterers.boardgames.auth.dto.ForgotPasswordRequest;
import com.motomutterers.boardgames.auth.dto.LoginRequest;
import com.motomutterers.boardgames.auth.dto.RegisterRequest;
import com.motomutterers.boardgames.auth.dto.ResetPasswordRequest;
import com.motomutterers.boardgames.auth.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
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
        return new ResponseEntity<String>("Account created, check your email to verify", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response){
        AuthResponse authResponse = authService.login(request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        HttpServletRequest request,
        HttpServletResponse response
    ){
        AuthResponse authResponse = authService.refresh(request, response);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
        HttpServletRequest request,
        HttpServletResponse response
    ){
        authService.logout(request, response);
        return ResponseEntity.ok("");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token){
        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request){
        authService.requestPasswordReset(request.getIsUsername(), request.getPrimaryKey());
        // Always 200, even if no account matched — don't reveal which accounts exist.
        return ResponseEntity.ok("If an account exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request){
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Password updated");
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(csrfToken);
    }
}
