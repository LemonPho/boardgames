package com.motomutterers.boardgames.auth.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.motomutterers.boardgames.auth.dto.GoogleIdentity;
import com.motomutterers.boardgames.auth.exceptions.GoogleAuthException;

// Verifies the ID token the browser gets from Google Identity Services.
//
// Everything downstream trusts this class, so it checks all four of: signature
// (against Google's published JWKS), issuer, audience (our client id — without
// this, a token minted for any other Google app would be accepted), and expiry.
@Service
public class GoogleTokenVerifier {
    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> VALID_ISSUERS = List.of(
        "https://accounts.google.com",
        "accounts.google.com"
    );

    private final JwtDecoder decoder;

    public GoogleTokenVerifier(
        @Value("${google.oauth.client-id}") String clientId
    ) {
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            audienceValidator(clientId),
            issuerValidator()
        ));
        this.decoder = nimbusDecoder;
    }

    // The token must have been issued for *this* app — without this check, a
    // token minted for any other Google client would be accepted.
    private static OAuth2TokenValidator<Jwt> audienceValidator(String clientId){
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(clientId)
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Invalid audience", null));
    }

    // Google documents two issuer spellings; both are legitimate.
    private static OAuth2TokenValidator<Jwt> issuerValidator(){
        return jwt -> jwt.getIssuer() != null && VALID_ISSUERS.contains(jwt.getIssuer().toString())
            ? OAuth2TokenValidatorResult.success()
            : OAuth2TokenValidatorResult.failure(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Invalid issuer", null));
    }

    public GoogleIdentity verify(String idToken){
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new GoogleAuthException("Google sign-in failed, please try again");
        }

        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        if(sub == null || sub.isBlank()){
            throw new GoogleAuthException("Google sign-in failed, please try again");
        }
        if(email == null || email.isBlank()){
            throw new GoogleAuthException("Your Google account has no email address");
        }
        // Boolean, not boolean: a missing claim is null, and unboxing it would
        // throw before this check could report anything useful.
        if(!Boolean.TRUE.equals(emailVerified)){
            throw new GoogleAuthException("Your email address isn't verified by google, complete your google registration");
        }

        return new GoogleIdentity(sub, email, true);
    }
}
