package com.motomutterers.boardgames.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdatePasswordRequest {
    @NotBlank
    private String currentPassword;

    @Pattern(
        regexp = "^(?=.*\\d)[a-zA-Z\\d@$!%*?&._-]{8,64}$",
        message = "Password must be at least 8 characters and contain at least one number"
    )
    private String newPassword;

    public UpdatePasswordRequest(){}

    public String getCurrentPassword(){return this.currentPassword;}
    public String getNewPassword(){return this.newPassword;}

    public void setCurrentPassword(String currentPassword){this.currentPassword = currentPassword;}
    public void setNewPassword(String newPassword){this.newPassword = newPassword;}
}
