import React, { createContext, useContext, useEffect, useRef, useState } from "react";
import { csrf, googleLogin, googleRegister, login, logout, refresh, register } from "../api/auth";
import type { AuthResponse, GoogleAuthResponse, LoginRequest, RegisterRequest } from "../types/auth";
import { useAlertsContext } from "./AlertsContext";
import type { GoogleRegisterErrors, LoginErrors, RegisterErrors } from "../types/components-types/auth";
import { setupInterceptors, setLoggingOut } from "../api/axiosSetup";

interface AuthenticationContextType {
  accessToken: string | null,
  registerUser: (request: RegisterRequest, setErrors: (errors: RegisterErrors | null) => void) => Promise<void>,
  loginUser: (request: LoginRequest, setErrors: (errors: LoginErrors | null) => void) => Promise<AuthResponse>,
  loginWithGoogle: (credential: string) => Promise<GoogleAuthResponse>,
  registerWithGoogle: (
    registrationToken: string,
    username: string,
    setErrors: (errors: GoogleRegisterErrors | null) => void
  ) => Promise<AuthResponse>,
  logoutUser: () => Promise<void>,
  csrfInit: () => Promise<void>,

  deleteAccessToken: () => void,
  restoreSession: () => Promise<void>
}

export const AuthenticationContext = createContext<AuthenticationContextType | null>(null);

export function AuthenticationContextProvider({ children }: { children: React.ReactNode }) {
  const { setErrorMessage } = useAlertsContext();

  const [loading, setLoading] = useState(true);

  const [accessToken, setAccessToken] = useState<string | null>(null);
  const tokenRef = useRef(accessToken);

  const registerUser = async (request: RegisterRequest, setErrors: (errors: RegisterErrors | null) => void): Promise<any> => {
    const response = await register(request, setErrors, setErrorMessage);
    return response;
  }

  const loginUser = async (request: LoginRequest, setErrors: (errors: LoginErrors | null) => void): Promise<AuthResponse> => {
    const response = await login(request, setErrors, setErrorMessage);
    if (response?.accessToken) {
      setAccessToken(response.accessToken);
    }

    return response;
  }

  // Signs in with a Google ID token. Returns the raw response so the caller can
  // branch: an existing account is now logged in, a new one needs a username.
  const loginWithGoogle = async (credential: string): Promise<GoogleAuthResponse> => {
    const response = await googleLogin(credential, setErrorMessage);
    if (response?.accessToken) {
      setAccessToken(response.accessToken);
    }

    return response;
  }

  // Completes a first-time Google sign-in, which logs them straight in.
  const registerWithGoogle = async (
    registrationToken: string,
    username: string,
    setErrors: (errors: GoogleRegisterErrors | null) => void
  ): Promise<AuthResponse> => {
    const response = await googleRegister(registrationToken, username, setErrors, setErrorMessage);
    if (response?.accessToken) {
      setAccessToken(response.accessToken);
    }

    return response;
  }

  const logoutUser = async (): Promise<void> => {
    // Suppress the interceptor's refresh→logout reaction to 401s that fire while
    // auth tears down. Clearing the token drives UserContext to clear the user.
    setLoggingOut(true);
    try {
      await logout();
      deleteAccessToken();
    } finally {
      setLoggingOut(false);
    }
  }

  const csrfInit = async (): Promise<void> => {
    await csrf(setErrorMessage);
  }

  const restoreSession = async (): Promise<void> => {
    try {
      const response = await refresh();
      if (response) {
        setAccessToken(response.accessToken);
      }
    } finally {
      setLoading(false);
    }
  }

  const deleteAccessToken = (): void => {
    setAccessToken(null);
  }

  useEffect(() => {
    tokenRef.current = accessToken;
  }, [accessToken]);

  useEffect(() => {
    const fetchData = async (): Promise<void> => {
      setupInterceptors(
        () => tokenRef.current,
        setAccessToken,
        logoutUser
      );

      await csrfInit();
      await restoreSession();
    }

    fetchData();
  }, []);

  if(loading) return null;

  return (
    <AuthenticationContext.Provider
      value={{ accessToken, registerUser, loginUser, loginWithGoogle, registerWithGoogle, logoutUser, csrfInit, deleteAccessToken, restoreSession }}
    >
      {children}
    </AuthenticationContext.Provider>
  )
}

export function useAuthenticationContext() {
  const ctx = useContext(AuthenticationContext);
  if (!ctx) throw new Error("Authentication context failed");
  return ctx;
}