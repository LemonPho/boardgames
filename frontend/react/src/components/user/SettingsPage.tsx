import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { updateUsername, updateEmail, updatePassword, deleteAccount } from "../../api/user";
import { useUserContext } from "../../context/UserContext";
import { useAuthenticationContext } from "../../context/AuthenticationContext";
import { useAlertsContext } from "../../context/AlertsContext";

/**
 * Account settings for the logged-in user: change username, email, password, or
 * delete the account. Each section submits independently. Requires a user; if
 * none is loaded, sends the visitor to login.
 */
export default function SettingsPage() {
  const { user, retrieveCurrentUser } = useUserContext();
  const { logoutUser } = useAuthenticationContext();
  const { setErrorMessage, setSuccessMessage } = useAlertsContext();
  const navigate = useNavigate();

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [emailPassword, setEmailPassword] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [busy, setBusy] = useState(false);

  if (!user) {
    return (
      <p className="max-w-lg mx-auto px-4 py-8 text-sm text-gray-500">
        You must be <Link to="/login" className="underline">logged in</Link> to manage settings.
      </p>
    );
  }

  const handleUsername = async (): Promise<void> => {
    if (!username.trim()) return;
    setBusy(true);
    try {
      await updateUsername(user.username, username.trim(), setErrorMessage);
      await retrieveCurrentUser();
      setSuccessMessage("Username updated");
      setUsername("");
    } catch { /* surfaced */ } finally { setBusy(false); }
  };

  const handleEmail = async (): Promise<void> => {
    if (!email.trim() || !emailPassword) return;
    setBusy(true);
    try {
      await updateEmail(user.username, email.trim(), emailPassword, setErrorMessage);
      // The email only changes once the new address confirms the link, so don't
      // refresh the user — just tell them to check the new inbox.
      setSuccessMessage("Check your new email to confirm the change");
      setEmail("");
      setEmailPassword("");
    } catch { /* surfaced */ } finally { setBusy(false); }
  };

  const handlePassword = async (): Promise<void> => {
    if (!currentPassword || !newPassword) return;
    setBusy(true);
    try {
      await updatePassword(user.username, currentPassword, newPassword, setErrorMessage);
      setSuccessMessage("Password updated — please sign in again");
      // Changing the password ends the current session; force re-login.
      await logoutUser();
      navigate("/login");
    } catch { /* surfaced */ } finally { setBusy(false); }
  };

  const handleDelete = async (): Promise<void> => {
    setBusy(true);
    try {
      await deleteAccount(user.username, setErrorMessage);
      setSuccessMessage("Account deleted");
      await logoutUser();
      navigate("/");
    } catch { /* surfaced */ } finally { setBusy(false); }
  };

  return (
    <div className="max-w-lg mx-auto px-4 py-8">
      <Link
        to={`/profile/${user.username}`}
        className="inline-flex items-center gap-1 text-sm text-gray-500 hover:text-gray-800 mb-6"
      >
        <ArrowLeft size={15} /> Profile
      </Link>

      <h1 className="text-2xl font-semibold text-gray-900 mb-8">Settings</h1>

      <div className="flex flex-col gap-6">
        {/* Username */}
        <Section title="Username" description={`Current: ${user.username}`}>
          <Field label="New username" value={username} onChange={setUsername} placeholder="New username" />
          <SaveButton onClick={handleUsername} disabled={busy || !username.trim()} />
        </Section>

        {/* Email */}
        <Section title="Email" description={`Current: ${user.email}`}>
          <Field label="New email" type="email" value={email} onChange={setEmail} placeholder="New email" />
          <Field label="Current password" type="password" value={emailPassword} onChange={setEmailPassword} placeholder="Confirm with your password" />
          <p className="text-xs text-gray-400">We'll send a confirmation link to the new address. Your email changes only after you confirm it.</p>
          <SaveButton onClick={handleEmail} disabled={busy || !email.trim() || !emailPassword} />
        </Section>

        {/* Password */}
        <Section title="Password">
          <Field label="Current password" type="password" value={currentPassword} onChange={setCurrentPassword} placeholder="Current password" />
          <Field label="New password" type="password" value={newPassword} onChange={setNewPassword} placeholder="New password" />
          <SaveButton onClick={handlePassword} disabled={busy || !currentPassword || !newPassword} />
        </Section>

        {/* Delete account */}
        <div className="rounded-xl border border-red-200 bg-red-50/40 p-4">
          <h2 className="text-base font-semibold text-red-700">Delete account</h2>
          <p className="text-sm text-gray-500 mt-1 mb-3">
            This permanently deletes your account. This can't be undone.
          </p>
          {!confirmDelete ? (
            <button
              onClick={() => setConfirmDelete(true)}
              className="text-sm font-medium px-4 py-2 rounded-lg border border-red-300 text-red-600 hover:bg-red-100 transition"
            >
              Delete my account
            </button>
          ) : (
            <div className="flex flex-col sm:flex-row gap-2">
              <button
                onClick={handleDelete}
                disabled={busy}
                className="text-sm font-medium px-4 py-2 rounded-lg bg-red-600 text-white hover:bg-red-700 transition disabled:opacity-40"
              >
                Yes, delete permanently
              </button>
              <button
                onClick={() => setConfirmDelete(false)}
                disabled={busy}
                className="text-sm font-medium px-4 py-2 rounded-lg border border-gray-200 text-gray-600 hover:border-gray-400 transition disabled:opacity-40"
              >
                Cancel
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Section({ title, description, children }: { title: string; description?: string; children: React.ReactNode }) {
  return (
    <div className="rounded-xl border border-gray-200 bg-white p-4">
      <h2 className="text-base font-semibold text-gray-800">{title}</h2>
      {description && <p className="text-sm text-gray-400 mt-0.5 mb-3 break-words">{description}</p>}
      <div className={`flex flex-col gap-3 ${description ? "" : "mt-3"}`}>{children}</div>
    </div>
  );
}

function Field({
  label, value, onChange, type = "text", placeholder,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  placeholder?: string;
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="text-xs font-medium text-gray-500">{label}</span>
      <input
        type={type}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-gray-400"
      />
    </label>
  );
}

function SaveButton({ onClick, disabled }: { onClick: () => void; disabled: boolean }) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className="self-start text-sm font-medium px-4 py-2 rounded-lg bg-gray-800 text-white hover:bg-gray-700 transition disabled:opacity-40"
    >
      Save
    </button>
  );
}
