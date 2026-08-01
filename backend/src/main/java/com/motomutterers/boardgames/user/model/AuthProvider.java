package com.motomutterers.boardgames.user.model;

// How an account was originally created. A LOCAL account that later links its
// Google identity stays LOCAL — it keeps its password. Use User.hasPassword()
// rather than this enum to decide whether password-guarded flows are available.
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
