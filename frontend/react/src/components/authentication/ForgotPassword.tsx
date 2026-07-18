import React, { useState } from "react";
import { Link } from "react-router-dom";
import { useAlertsContext } from "../../context/AlertsContext";
import { forgotPassword } from "../../api/auth";

export default function ForgotPassword() {
  const { setErrorMessage } = useAlertsContext();

  const [primaryKeyInput, setPrimaryKeyInput] = useState<string>("");
  const [busy, setBusy] = useState(false);
  const [sent, setSent] = useState(false);

  const handlePrimaryKeyInput = (event: React.ChangeEvent<HTMLInputElement>): void => {
    setPrimaryKeyInput(event.target.value);
  };

  const submitFindAccount = async (): Promise<void> => {
    if (!primaryKeyInput.trim()) return;
    // An "@" means they typed an email; otherwise treat it as a username.
    const isUsername = !primaryKeyInput.includes("@");

    setBusy(true);
    try {
      await forgotPassword(isUsername, primaryKeyInput.trim(), setErrorMessage);
      // Always show the same confirmation — the backend won't reveal whether the
      // account exists, and neither do we.
      setSent(true);
    } catch {
      /* surfaced via alerts */
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-lg w-full max-w-md p-6">
        {sent ? (
          <div className="text-center">
            <div className="w-12 h-12 mx-auto rounded-full bg-green-100 text-green-600 flex items-center justify-center text-xl mb-3">
              ✓
            </div>
            <h1 className="text-xl font-semibold text-gray-900 mb-1">Check your email</h1>
            <p className="text-sm text-gray-500 mb-4">
              If an account matches that, we've sent a link to reset your password. The link expires in 15 minutes.
            </p>
            <Link to="/login" className="text-sm font-medium text-gray-700 hover:text-gray-900 underline">
              Back to login
            </Link>
          </div>
        ) : (
          <>
            <h1 className="text-xl font-semibold text-gray-900 mb-1">Forgot password</h1>
            <p className="text-sm text-gray-500 mb-5">
              Enter your username or email and we'll send you a reset link.
            </p>

            <label className="flex flex-col gap-1 mb-4">
              <span className="text-xs font-medium text-gray-500">Username or email</span>
              <input
                type="text"
                value={primaryKeyInput}
                onChange={handlePrimaryKeyInput}
                placeholder="Username or email"
                onKeyDown={(e) => e.key === "Enter" && submitFindAccount()}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
              />
            </label>

            <button
              onClick={submitFindAccount}
              disabled={busy || !primaryKeyInput.trim()}
              className="w-full text-sm font-medium py-2.5 rounded-lg bg-gray-800 text-white hover:bg-gray-700 transition disabled:opacity-40"
            >
              {busy ? "Sending…" : "Send reset link"}
            </button>

            <div className="text-center mt-4">
              <Link to="/login" className="text-sm text-gray-500 hover:text-gray-800 underline">
                Back to login
              </Link>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
