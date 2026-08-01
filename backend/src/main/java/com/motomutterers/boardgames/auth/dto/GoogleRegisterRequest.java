package com.motomutterers.boardgames.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Completes a Google sign-up: the registration token from /auth/google/login plus the
// username the player picked. Same username rules as local registration.
public class GoogleRegisterRequest {
    @NotBlank
    private String registrationToken;

    @Pattern(
        regexp = "^[a-zA-Z0-9 ._-]{3,18}$",
        message = "Username must be 3-18 characters and can only contain letters, numbers, spaces, periods, underscores and hyphens"
    )
    private String username;

    public GoogleRegisterRequest(){}

    public GoogleRegisterRequest(String registrationToken, String username){
        this.registrationToken = registrationToken;
        this.username = username;
    }

    public String getRegistrationToken(){
        return registrationToken;
    }

    public String getUsername(){
        return username;
    }

    public void setRegistrationToken(String registrationToken){
        this.registrationToken = registrationToken;
    }

    public void setUsername(String username){
        this.username = username;
    }
}
