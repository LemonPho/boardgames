import { useState } from "react";
import { useSearchParams, useNavigate, Link } from "react-router-dom";
import { useAlertsContext } from "../../context/AlertsContext";
import { resetPassword } from "../../api/auth";

/**
 * Landing page for the password-reset link (/reset-password?token=...). Takes a
 * new password and applies it via the emailed token, then sends the user to login.
 */
export default function CreateNewPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const { setErrorMessage, setSuccessMessage } = useAlertsContext();
  const navigate = useNavigate();

  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);

  const mismatch = confirm.length > 0 && password !== confirm;
  const canSubmit = !!token && password.length > 0 && password === confirm && !busy;

  const submit = async (): Promise<void> => {
    if (!token) {
      setErrorMessage("This reset link is invalid.");
      return;
    }
    if (password !== confirm) return;

    setBusy(true);
    try {
      await resetPassword(token, password, setErrorMessage);
      setSuccessMessage("Password updated — please sign in");
      navigate("/login");
    } catch {
      /* surfaced via alerts */
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-md p-6">
        {!token ? (
          <div className="text-center">
            <h1 className="text-xl font-semibold text-gray-900 mb-1">Invalid link</h1>
            <p className="text-sm text-gray-500 mb-4">This password reset link is missing or invalid.</p>
            <Link to="/forgot-password" className="text-sm font-medium text-gray-700 hover:text-gray-900 underline">
              Request a new link
            </Link>
          </div>
        ) : (
          <>
            <h1 className="text-xl font-semibold text-gray-900 mb-1">Set a new password</h1>
            <p className="text-sm text-gray-500 mb-5">Choose a new password for your account.</p>

            <label className="flex flex-col gap-1 mb-3">
              <span className="text-xs font-medium text-gray-500">New password</span>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="New password"
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
              />
            </label>

            <label className="flex flex-col gap-1 mb-1">
              <span className="text-xs font-medium text-gray-500">Confirm password</span>
              <input
                type="password"
                value={confirm}
                onChange={(e) => setConfirm(e.target.value)}
                placeholder="Confirm password"
                onKeyDown={(e) => e.key === "Enter" && canSubmit && submit()}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
              />
            </label>
            <p className="h-4 text-xs text-red-500 mb-3">{mismatch ? "Passwords don't match" : ""}</p>

            <button
              onClick={submit}
              disabled={!canSubmit}
              className="w-full text-sm font-medium py-2.5 rounded-lg bg-gray-800 text-white hover:bg-gray-700 transition disabled:opacity-40"
            >
              {busy ? "Saving…" : "Update password"}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
