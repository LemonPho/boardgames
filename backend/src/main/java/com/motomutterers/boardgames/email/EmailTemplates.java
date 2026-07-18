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

    public static String emailChangeEmail(String username, String verificationLink) {
        return """
                <h2>Confirm your new email</h2>
                <p>Hi %s,</p>
                <p>Confirm this address to finish changing the email on your Boardgames account:</p>
                <a href="%s">Confirm new email</a>
                <p>This link expires in 15 minutes.</p>
                <p>If you did not request this change, you can ignore this email — your address won't change.</p>
                """.formatted(username, verificationLink);
    }

    public static String roomInvitationEmail(String username, String roomInvitationLink) {
        return """
                <h2>You've been invited to a room!</h2>
                <p>Hi %s,</p>
                <p>You can accept the invitation clinking the link below:</p>
                <a href="%s">Accept invitation</a>
                <p>This link expires in 10 minutes.</p>
                """.formatted(username, roomInvitationLink);
    }
}