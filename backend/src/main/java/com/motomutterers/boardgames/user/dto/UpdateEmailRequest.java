package com.motomutterers.boardgames.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UpdateEmailRequest {
    @Email
    @NotBlank
    private String email;

    // Confirming the current password guards against a hijacked session changing
    // the recovery address.
    @NotBlank
    private String currentPassword;

    public UpdateEmailRequest(){}

    public String getEmail(){return this.email;}
    public String getCurrentPassword(){return this.currentPassword;}

    public void setEmail(String email){this.email = email;}
    public void setCurrentPassword(String currentPassword){this.currentPassword = currentPassword;}
}
