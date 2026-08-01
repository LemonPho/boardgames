export interface RegisterErrors{
    username: string,
    email: string,
    password: string
}

export interface LoginErrors{
    userExists: string,
    password: string
}

// Only the username can fail when completing a Google sign-up — the email and
// identity come from the (already verified) Google token.
export interface GoogleRegisterErrors{
    username: string
}

// Login/register forms host the Google button but don't own the username step;
// they hand a new Google identity up to AuthPage, which swaps in that form.
export interface GoogleRegistrationHandoff{
    onGoogleRegistrationRequired: (registrationToken: string, email: string) => void
}