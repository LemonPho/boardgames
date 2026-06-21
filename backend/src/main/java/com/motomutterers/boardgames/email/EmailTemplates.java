package com.motomutterers.boardgames.email;

public class EmailTemplates {

    public static String verificationEmail(String username, String verificationLink) {
        return """
                <h2>Welcome to Boardgames!</h2>
                <p>Hi %s,</p>
                <p>Please verify your email address by clicking the link below:</p>
                <a href="%s">Verify my account</a>
                <p>This link expires in 15 minutes.</p>
                <p>If you did not create an account, you can ignore this email.</p>
                """.formatted(username, verificationLink);
    }
}