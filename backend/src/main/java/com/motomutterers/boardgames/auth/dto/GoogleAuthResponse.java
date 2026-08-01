package com.motomutterers.boardgames.auth.dto;

// Result of POST /auth/google/login. Two outcomes:
//
//  - existing account  -> registrationRequired = false, accessToken set.
//                         Identical to a normal login from the client's view.
//  - first time here   -> registrationRequired = true, accessToken null, and
//                         registrationToken + email set. The client collects a
//                         username and posts it to /auth/google/register.
//
// No user row is created until the username is chosen, so an abandoned sign-up
// leaves nothing behind.
public class GoogleAuthResponse {
    private boolean registrationRequired;
    private String accessToken;
    private String registrationToken;
    private String email;

    private GoogleAuthResponse(){}

    public static GoogleAuthResponse loggedIn(String accessToken){
        GoogleAuthResponse response = new GoogleAuthResponse();
        response.registrationRequired = false;
        response.accessToken = accessToken;
        return response;
    }

    public static GoogleAuthResponse registrationRequired(
        String registrationToken,
        String email
    ){
        GoogleAuthResponse response = new GoogleAuthResponse();
        response.registrationRequired = true;
        response.registrationToken = registrationToken;
        response.email = email;
        return response;
    }

    public boolean getRegistrationRequired(){
        return registrationRequired;
    }

    public String getAccessToken(){
        return accessToken;
    }

    public String getRegistrationToken(){
        return registrationToken;
    }

    public String getEmail(){
        return email;
    }
}
