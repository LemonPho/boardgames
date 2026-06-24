import { useState } from 'react'
import { register } from '../../api/auth'
import type { RegisterRequest } from '../../types/auth'
import { Link } from 'react-router-dom'

export default function RegisterForm() {
  const [form, setForm] = useState<RegisterRequest>({
    email: '',
    username: '',
    password: ''
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [success, setSuccess] = useState<string | null>(null)

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrors({})
    setSuccess(null)

    try {
      const message = await register(form)
      setSuccess(message)
    } catch (error: any) {
      if (error.response?.data) {
        const data = error.response.data
        if (typeof data === 'string') {
          setErrors({ general: data })
        } else {
          setErrors(data)
        }
      }
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-gray-800">Create an account</h1>
        <p className="text-sm text-gray-400 mt-1">Start tracking your games today</p>
      </div>

      {success && (
        <div className="bg-green-50 border border-green-200 text-green-700 text-sm px-4 py-3 rounded-lg">
          {success}
        </div>
      )}

      {errors.general && (
        <div className="bg-red-50 border border-red-200 text-red-600 text-sm px-4 py-3 rounded-lg">
          {errors.general}
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
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
          {errors.email && <p className="text-xs text-red-500">{errors.email}</p>}
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
          {errors.username && <p className="text-xs text-red-500">{errors.username}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <label className="text-sm font-medium text-gray-700">Password</label>
          <input
            name="password"
            type="password"
            placeholder="••••••••"
            value={form.password}
            onChange={handleChange}
            className="border border-gray-200 rounded-lg px-4 py-2.5 text-sm text-gray-800 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-300 transition"
          />
          {errors.password && <p className="text-xs text-red-500">{errors.password}</p>}
        </div>

        <button
          type="submit"
          className="bg-gray-800 hover:bg-gray-700 text-white text-sm font-medium py-2.5 rounded-lg transition mt-2"
        >
          Create account
        </button>
      </form>

      <p className="text-sm text-gray-400 text-center">
        Already have an account?{' '}
        <Link to="/login" className="text-gray-700 font-medium hover:underline">
          Sign in
        </Link>
      </p>
    </div>
  )
}