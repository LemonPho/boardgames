import { useState } from 'react'
import type { LoginRequest } from '../../types/auth'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthenticationContext } from '../../context/AuthenticationContext'
import type { LoginErrors } from '../../types/components-types/auth';
import { useAlertsContext } from '../../context/AlertsContext';
import SubmitButton from '../util/SubmitButton';

export default function LoginForm() {
  const { errorMessage } = useAlertsContext();
  const { loginUser } = useAuthenticationContext();

  const [form, setForm] = useState<LoginRequest>({
    primaryKey: '',
    password: '',
    isUsername: false
  });
  const [errors, setErrors] = useState<LoginErrors | null>(null);
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  // SubmitButton owns the loading lifecycle; this just does the work.
  const handleSubmit = async (): Promise<void> => {
    setErrors(null);

    // "@" means an email was typed; otherwise it's a username.
    const inputIsUsername = !form.primaryKey.includes("@");
    await loginUser({ ...form, isUsername: inputIsUsername }, setErrors);
    navigate("/");
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-gray-800">Welcome back</h1>
        <p className="text-sm text-gray-400 mt-1">Sign in to your account</p>
      </div>

      {errorMessage && (
        <div className="bg-red-50 border border-red-200 text-red-600 text-sm px-4 py-3 rounded-lg">
          {errorMessage}
        </div>
      )}

      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-gray-700">Email or username</label>
          <input
            name="primaryKey"
            type="text"
            placeholder="you@example.com"
            value={form.primaryKey}
            onChange={handleChange}
            className="border border-gray-200 rounded-lg px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-300 transition"
          />
          {errors && errors.userExists && <p className="text-xs text-red-500">{errors.userExists}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <div className="flex justify-between items-center">
            <label className="text-sm font-medium text-gray-700">Password</label>
            <Link to="/forgot-password" className="text-xs text-gray-500 hover:text-gray-800 hover:underline">
              Forgot password?
            </Link>
          </div>
          <input
            name="password"
            type="password"
            placeholder="••••••••"
            value={form.password}
            onChange={handleChange}
            onKeyDown={(e) => e.key === "Enter" && !loading && handleSubmit()}
            className="border border-gray-200 rounded-lg px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-300 transition"
          />
          {errors && errors.password && <p className="text-xs text-red-500">{errors.password}</p>}
        </div>

        <SubmitButton
          text="Sign in"
          loading={loading}
          setLoading={setLoading}
          onSubmit={handleSubmit}
          className="bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium py-2.5 rounded-lg transition mt-2 disabled:opacity-40"
        />
      </div>

      <p className="text-sm text-gray-400 text-center">
        Don't have an account?{' '}
        <Link to="/register" className="text-gray-700 font-medium hover:underline">
          Create one
        </Link>
      </p>
    </div>
  )
}