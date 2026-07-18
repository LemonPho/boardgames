package com.motomutterers.boardgames.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Completes a password reset: the emailed token plus the new password. */
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @Pattern(
        regexp = "^(?=.*\\d)[a-zA-Z\\d@$!%*?&._-]{8,64}$",
        message = "Password must be at least 8 characters and contain at least one number"
    )
    private String newPassword;

    public ResetPasswordRequest(){}

    public String getToken(){return this.token;}
    public String getNewPassword(){return this.newPassword;}

    public void setToken(String token){this.token = token;}
    public void setNewPassword(String newPassword){this.newPassword = newPassword;}
}
