import { useState } from 'react'
import skullKingImage from '../../assets/skullking/skull-king-1-jeux-Toulon-L-Ataniere.webp'

import LoginForm from "./LoginForm"
import RegisterForm from "./RegisterForm"
import GoogleUsernameForm from "./GoogleUsernameForm"
import GoogleAuthProvider from "./GoogleAuthProvider"
import type { AuthProps } from "../../types/auth"

export default function AuthPage({ tab }: AuthProps) {
  // Set when Google sign-in succeeded for an identity with no account yet: the
  // username step replaces the login/register form until it's done or cancelled.
  const [googleRegistration, setGoogleRegistration] = useState<{ token: string; email: string } | null>(null);

  const onRegistrationRequired = (token: string, email: string) =>
    setGoogleRegistration({ token, email });

  return (
    <div className="bg-white rounded-2xl shadow-lg flex overflow-hidden w-full max-w-2xl">

      {/* Left side - form */}
      <div className="w-full md:w-1/2 p-10 flex flex-col justify-center">

        {/* Form content */}
        {googleRegistration ? (
          // No Google widget here — this step talks to our own API, so it sits
          // outside the provider.
          <GoogleUsernameForm
            registrationToken={googleRegistration.token}
            email={googleRegistration.email}
            onCancel={() => setGoogleRegistration(null)}
          />
        ) : (
          <GoogleAuthProvider>
            {tab === 'login' ? (
              <LoginForm onGoogleRegistrationRequired={onRegistrationRequired} />
            ) : (
              <RegisterForm onGoogleRegistrationRequired={onRegistrationRequired} />
            )}
          </GoogleAuthProvider>
        )}
      </div>

      {/* Right side - image */}
      <div className="hidden md:block w-1/2">
        <img src={skullKingImage} alt="Board games" className="w-full h-full object-cover rounded-2xl p-2" />
      </div>

    </div>
  )
}
