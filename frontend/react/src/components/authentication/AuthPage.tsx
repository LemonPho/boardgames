import skullKingImage from '../../assets/skullking/skull-king-1-jeux-Toulon-L-Ataniere.webp'

import LoginForm from "./LoginForm"
import RegisterForm from "./RegisterForm"
import type { AuthProps } from "../../types/auth"

export default function AuthPage({ tab }: AuthProps) {
  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-lg flex overflow-hidden w-full max-w-2xl">
        
        {/* Left side - form */}
        <div className="w-full md:w-1/2 p-10 flex flex-col justify-center">

          {/* Form content */}
          {tab === 'login' ? <LoginForm /> : <RegisterForm />}
        </div>

        {/* Right side - image */}
        <div className="hidden md:block w-1/2">
          <img src={skullKingImage} alt="Board games" className="w-full h-full object-cover rounded-2xl p-2" />
        </div>

      </div>
    </div>
  )
}