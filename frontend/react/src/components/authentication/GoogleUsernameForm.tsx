import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthenticationContext } from "../../context/AuthenticationContext";
import { useAlertsContext } from "../../context/AlertsContext";
import type { GoogleRegisterErrors } from "../../types/components-types/auth";
import SubmitButton from "../util/SubmitButton";

interface GoogleUsernameFormProps {
  // Carries the verified Google identity from the sign-in step.
  registrationToken: string;
  // The Google address the account will be created with — shown, not editable.
  email: string;
  onCancel: () => void;
}

/**
 * Second half of a first-time Google sign-in. Google gives us no username, and
 * ours is public (it's how players appear in rooms), so the player picks one
 * here. The account is only created when this submits — abandoning the form
 * leaves nothing behind.
 */
export default function GoogleUsernameForm({ registrationToken, email, onCancel }: GoogleUsernameFormProps) {
  const { registerWithGoogle } = useAuthenticationContext();
  const { errorMessage } = useAlertsContext();

  const [username, setUsername] = useState("");
  const [errors, setErrors] = useState<GoogleRegisterErrors | null>(null);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const handleSubmit = async (): Promise<void> => {
    setErrors(null);
    await registerWithGoogle(registrationToken, username.trim(), setErrors);
    // Registering signs them in, so go straight to the app.
    navigate("/");
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-gray-800">Choose a username</h1>
        <p className="text-sm text-gray-400 mt-1">
          This is how other players will see you{email && <> — signing up as {email}</>}
        </p>
      </div>

      {errorMessage && (
        <div className="bg-red-50 border border-red-200 text-red-600 text-sm px-4 py-3 rounded-lg">
          {errorMessage}
        </div>
      )}

      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-gray-700">Username</label>
          <input
            name="username"
            type="text"
            placeholder="Your username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && !loading && username.trim() && handleSubmit()}
            autoFocus
            className="border border-gray-200 rounded-lg px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-300 transition"
          />
          {errors && errors.username && <p className="text-xs text-red-500">{errors.username}</p>}
        </div>

        <SubmitButton
          text="Create account"
          loading={loading}
          setLoading={setLoading}
          onSubmit={handleSubmit}
          disabled={!username.trim()}
          className="bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium py-2.5 rounded-lg transition mt-2 disabled:opacity-40"
        />
      </div>

      <button
        onClick={onCancel}
        disabled={loading}
        className="text-sm text-gray-400 hover:text-gray-700 transition disabled:opacity-40"
      >
        Cancel
      </button>
    </div>
  );
}
