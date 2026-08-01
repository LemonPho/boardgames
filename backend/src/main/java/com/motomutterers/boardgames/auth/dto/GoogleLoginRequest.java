package com.motomutterers.boardgames.auth.dto;

import jakarta.validation.constraints.NotBlank;

// The ID token ("credential") handed to the browser by Google Identity Services.
public class GoogleLoginRequest {
    @NotBlank
    private String credential;

    public GoogleLoginRequest(){}

    public GoogleLoginRequest(String credential){
        this.credential = credential;
    }

    public String getCredential(){
        return credential;
    }

    public void setCredential(String credential){
        this.credential = credential;
    }
}
