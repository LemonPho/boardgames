package com.motomutterers.boardgames.auth.services;

import com.motomutterers.boardgames.auth.dto.GoogleIdentity;
import com.motomutterers.boardgames.auth.exceptions.GoogleAuthException;
import com.motomutterers.boardgames.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Window for a Google sign-up to pick a username. Long enough to type one,
    // short enough that a leaked token is worthless.
    // Defaulted because it lives in the untracked per-machine
    // application.properties: a deploy that hasn't added the key still boots.
    @Value("${jwt.google-registration-expiration:900}")
    private long googleRegistrationExpiration;

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration*1000))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateGoogleRegistrationToken(GoogleIdentity identity) {
        return Jwts.builder()
                .subject(identity.sub())
                .claim("purpose", "GOOGLE_REGISTRATION")
                .claim("email", identity.email())
                .claim("emailVerified", identity.emailVerified())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + googleRegistrationExpiration*1000))
                .signWith(getSigningKey())
                .compact();
    }

    public GoogleIdentity parseGoogleRegistrationToken(String token) {
        Claims claims;
        try {
            claims = parseClaims(token);
        } catch (Exception e) {
            throw new GoogleAuthException("Your sign-up session expired, please sign in with Google again");
        }

        if(!"GOOGLE_REGISTRATION".equals(claims.get("purpose", String.class))){
            throw new GoogleAuthException("Your sign-up session expired, please sign in with Google again");
        }

        return new GoogleIdentity(
            claims.getSubject(),
            claims.get("email", String.class),
            Boolean.TRUE.equals(claims.get("emailVerified", Boolean.class))
        );
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.get("purpose", String.class) == null;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}