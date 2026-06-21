package com.motomutterers.boardgames.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class RegisterRequest {
    @Pattern(
        regexp = "^[a-zA-Z0-9 ._-]{3,12}$",
        message = "Username must be 3-12 characters and can only contain letters, numbers, spaces, periods, underscores and hyphens"
    )
    private String username;

    @Email
    @NotBlank
    private String email;

    @Pattern(
        regexp = "^(?=.*\\d)[a-zA-Z\\d@$!%*?&._-]{8,64}$",
        message = "Password must be at least 8 characters and contain at least one number"
    )
    private String password;

    public RegisterRequest(
        String username, 
        String email,
        String password
    ) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public String getUsername(){
        return username;
    }

    public String getEmail(){
        return email;
    }

    public String getPassword(){
        return password;
    }

    public void setUsername(String username){
        this.username = username;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public void setPassword(String password){
        this.password = password;
    }
}
