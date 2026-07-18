import { useEffect, useState } from "react";
import { useSearchParams, Link } from "react-router-dom";
import { verifyEmail } from "../../api/auth";
import { useAlertsContext } from "../../context/AlertsContext";

type Status = "verifying" | "success" | "error";

/**
 * Landing page for the email verification link (/verify?token=...). Handles both
 * account activation and email-change confirmation — the backend applies the
 * right action based on the token. Shows the result and a link onward.
 */
export default function VerifyPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");
  const { setErrorMessage } = useAlertsContext();

  const [status, setStatus] = useState<Status>("verifying");

  useEffect(() => {
    if (!token) {
      setStatus("error");
      return;
    }
    let cancelled = false;
    (async () => {
      try {
        await verifyEmail(token, setErrorMessage);
        if (!cancelled) setStatus("success");
      } catch {
        if (!cancelled) setStatus("error");
      }
    })();
    return () => { cancelled = true; };
  }, [token]);

  return (
    <div className="max-w-md mx-auto px-4 py-16 text-center">
      {status === "verifying" && (
        <p className="text-sm text-gray-500">Verifying…</p>
      )}

      {status === "success" && (
        <div className="flex flex-col items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-green-100 text-green-600 flex items-center justify-center text-xl">✓</div>
          <h1 className="text-xl font-semibold text-gray-900">Email verified</h1>
          <p className="text-sm text-gray-500">Your email has been confirmed.</p>
          <Link to="/login" className="mt-2 text-sm font-medium px-4 py-2 rounded-lg bg-gray-800 text-white hover:bg-gray-700 transition">
            Continue to login
          </Link>
        </div>
      )}

      {status === "error" && (
        <div className="flex flex-col items-center gap-3">
          <div className="w-12 h-12 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-xl">✕</div>
          <h1 className="text-xl font-semibold text-gray-900">Verification failed</h1>
          <p className="text-sm text-gray-500">This link is invalid or has expired.</p>
          <Link to="/" className="mt-2 text-sm font-medium px-4 py-2 rounded-lg border border-gray-200 text-gray-600 hover:border-gray-400 transition">
            Back home
          </Link>
        </div>
      )}
    </div>
  );
}
