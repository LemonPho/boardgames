import { useState } from 'react'
import { register } from '../../api/auth'
import type { RegisterRequest } from '../../types/auth'

export default function RegisterPage() {
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

  const handleSubmit = async (e: React.SubmitEvent) => {
    e.preventDefault()
    setErrors({})
    setSuccess(null)

    try {
      const message = await register(form)
      setSuccess(message)
    } catch (error: any) {
      if (error.response?.data) {
        setErrors(error.response.data)
      }
    }
  }

  return (
    <div>
      <h1>Register</h1>

      {success && <p>{success}</p>}

      <form onSubmit={handleSubmit}>
        <div>
          <input
            name="email"
            type="email"
            placeholder="Email"
            value={form.email}
            onChange={handleChange}
          />
          {errors.email && <p>{errors.email}</p>}
        </div>

        <div>
          <input
            name="username"
            type="text"
            placeholder="Username"
            value={form.username}
            onChange={handleChange}
          />
          {errors.username && <p>{errors.username}</p>}
        </div>

        <div>
          <input
            name="password"
            type="password"
            placeholder="Password"
            value={form.password}
            onChange={handleChange}
          />
          {errors.password && <p>{errors.password}</p>}
        </div>

        <button type="submit">Register</button>
      </form>
    </div>
  )
}