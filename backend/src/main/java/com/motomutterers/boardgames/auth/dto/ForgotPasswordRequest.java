package com.motomutterers.boardgames.auth.dto;

/**
 * Starts a password reset. primaryKey is the username or email the user typed;
 * isUsername tells us which (same shape as LoginRequest).
 */
public class ForgotPasswordRequest {
    private boolean isUsername;
    private String primaryKey;

    public ForgotPasswordRequest(){}

    public ForgotPasswordRequest(boolean isUsername, String primaryKey){
        this.isUsername = isUsername;
        this.primaryKey = primaryKey;
    }

    public boolean getIsUsername(){return this.isUsername;}
    public String getPrimaryKey(){return this.primaryKey;}

    public void setIsUsername(boolean isUsername){this.isUsername = isUsername;}
    public void setPrimaryKey(String primaryKey){this.primaryKey = primaryKey;}
}
