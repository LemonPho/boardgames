import { useState } from 'react'
import type { RegisterRequest } from '../../types/auth'
import { Link } from 'react-router-dom'
import { useAlertsContext } from '../../context/AlertsContext'
import { useAuthenticationContext } from '../../context/AuthenticationContext';
import type { RegisterErrors } from '../../types/components-types/auth';
import SubmitButton from '../util/SubmitButton';

export default function RegisterForm() {
  const { errorMessage, setSuccessMessage, successMessage } = useAlertsContext();
  const { registerUser } = useAuthenticationContext();

  const [errors, setErrors] = useState<RegisterErrors | null>(null);
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState<RegisterRequest>({
    email: '',
    username: '',
    password: ''
  })

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  // SubmitButton owns the loading lifecycle; this just does the work.
  const handleSubmit = async (): Promise<void> => {
    setErrors(null);
    await registerUser(form, setErrors);
    setSuccessMessage("Account created successfully, check your email to validate.");
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-gray-800">Create an account</h1>
        <p className="text-sm text-gray-400 mt-1">Start tracking your games today</p>
      </div>

      {successMessage && (
        <div className="bg-green-50 border border-green-200 text-green-700 text-sm px-4 py-3 rounded-lg">
          {successMessage}
        </div>
      )}

      {errorMessage && (
        <div className="bg-red-50 border border-red-200 text-red-600 text-sm px-4 py-3 rounded-lg">
          {errorMessage}
        </div>
      )}

      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-gray-700">Email</label>
          <input
            name="email"
            type="email"
            placeholder="you@example.com"
            value={form.email}
            onChange={handleChange}
            className="border border-gray-200 rounded-lg px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-300 transition"
          />
          {errors && errors.email && <p className="text-xs text-red-500">{errors.email}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-gray-700">Username</label>
          <input
            name="username"
            type="text"
            placeholder="Your username"
            value={form.username}
            onChange={handleChange}
            className="border border-gray-200 rounded-lg px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-300 transition"
          />
          {errors && errors.username && <p className="text-xs text-red-500">{errors.username}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-gray-700">Password</label>
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
          text="Create account"
          loading={loading}
          setLoading={setLoading}
          onSubmit={handleSubmit}
          className="bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium py-2.5 rounded-lg transition mt-2 disabled:opacity-40"
        />
      </div>

      <p className="text-sm text-gray-400 text-center">
        Already have an account?{' '}
        <Link to="/login" className="text-gray-700 font-medium hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  )
}