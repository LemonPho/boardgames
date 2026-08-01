import { GoogleOAuthProvider } from "@react-oauth/google";

/**
 * Provides Google Identity Services to the auth page, reading the client id from
 * the environment itself so callers don't have to thread it through. Scoped to
 * AuthPage — the login and register forms are the only place Google sign-in is
 * offered, so the rest of the app doesn't need Google's script loaded.
 *
 * The client id is not a secret: it ships in this bundle by design and is only
 * an audience label. The backend verifies ID tokens against Google's public keys
 * and checks this same id as the audience, so the trust lives there.
 */
export default function GoogleAuthProvider({ children }: { children: React.ReactNode }) {
  const clientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  // Without a client id the Google button can't render. Rather than crash the
  // whole app, carry on without it — the password forms still work.
  if (!clientId) {
    console.error("VITE_GOOGLE_CLIENT_ID is not set — Google sign-in is unavailable");
    return children;
  }

  return <GoogleOAuthProvider clientId={clientId}>{children}</GoogleOAuthProvider>;
}
