import { useState } from 'react'
import { login } from '../../api/auth'
import type { LoginRequest } from '../../types/auth'
import { Link, useNavigate } from 'react-router-dom'

export default function LoginForm() {
  const [form, setForm] = useState<LoginRequest>({
    primaryKey: '',
    password: '',
    isUsername: false
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const navigate = useNavigate()

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value })
  }

  const setIsUsername = (value: boolean) => {
    setForm({
        ...form,
        isUsername: value
    })
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrors({})

    const inputIsUsername = !form.primaryKey.includes("@");
    setIsUsername(inputIsUsername);

    console.log(form);

    try {
      await login(form)
      navigate('/')
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
        <h1 className="text-2xl font-semibold text-gray-800">Welcome back</h1>
        <p className="text-sm text-gray-400 mt-1">Sign in to your account</p>
      </div>

      {errors.general && (
        <div className="bg-red-50 border border-red-200 text-red-600 text-sm px-4 py-3 rounded-lg">
          {errors.general}
        </div>
      )}

      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
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
          {errors.primaryKey && <p className="text-xs text-red-500">{errors.primaryKey}</p>}
        </div>

        <div className="flex flex-col gap-1">
          <div className="flex justify-between items-center">
            <label className="text-sm font-medium text-gray-700">Password</label>
          </div>
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
          Sign in
        </button>
      </form>

      <p className="text-sm text-gray-400 text-center">
        Don't have an account?{' '}
        <Link to="/register" className="text-gray-700 font-medium hover:underline">
          Create one
        </Link>
      </p>
    </div>
  )
}