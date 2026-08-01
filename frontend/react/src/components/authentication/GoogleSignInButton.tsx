import { GoogleLogin } from "@react-oauth/google";
import { useNavigate } from "react-router-dom";
import { useAuthenticationContext } from "../../context/AuthenticationContext";
import { useAlertsContext } from "../../context/AlertsContext";

interface GoogleSignInButtonProps {
  // Shown on the button itself ("signin_with" | "signup_with" | "continue_with").
  text?: "signin_with" | "signup_with" | "continue_with";
  // Called when this Google identity has no account yet: the parent collects a
  // username and finishes via registerWithGoogle.
  onRegistrationRequired: (registrationToken: string, email: string) => void;
}

/**
 * Google Identity Services button. On success Google hands us a signed ID token
 * ("credential") which the backend verifies; from there the user either lands
 * logged in (existing account) or is sent to pick a username (new one).
 *
 * Rendered by Google in an iframe, so it can't use SubmitButton — the loading
 * lifecycle and styling belong to Google's widget.
 */
export default function GoogleSignInButton({
  text = "continue_with",
  onRegistrationRequired,
}: GoogleSignInButtonProps) {
  const { loginWithGoogle } = useAuthenticationContext();
  const { setErrorMessage, clearAlerts } = useAlertsContext();
  const navigate = useNavigate();

  const handleSuccess = async (credential?: string): Promise<void> => {
    if (!credential) {
      setErrorMessage("Google sign-in failed, please try again");
      return;
    }

    clearAlerts();
    try {
      const response = await loginWithGoogle(credential);
      if (response.registrationRequired) {
        onRegistrationRequired(response.registrationToken!, response.email ?? "");
        return;
      }
      // Token is set; UserContext picks the user up from it.
      navigate("/");
    } catch { /* surfaced by loginWithGoogle */ }
  };

  return (
    <div className="flex justify-center">
      <GoogleLogin
        text={text}
        width="280"
        onSuccess={(response) => handleSuccess(response.credential)}
        onError={() => setErrorMessage("Google sign-in failed, please try again")}
      />
    </div>
  );
}
