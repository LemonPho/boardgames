import React, { createContext, useContext, useEffect, useRef, useState } from "react";
import { csrf, login, logout, refresh, register } from "../api/auth";
import type { AuthResponse, LoginRequest, RegisterRequest } from "../types/auth";
import { useAlertsContext } from "./AlertsContext";
import type { LoginErrors, RegisterErrors } from "../types/components-types/auth";
import { ACCESS_TOKEN, setupInterceptors } from "../api/axiosSetup";

interface AuthenticationContextType {
  accessToken: string | null,
  registerUser: (request: RegisterRequest, setErrors: (errors: RegisterErrors | null) => void) => Promise<void>,
  loginUser: (request: LoginRequest, setErrors: (errors: LoginErrors | null) => void) => Promise<AuthResponse>,
  logoutUser: () => Promise<void>,
  csrfInit: () => Promise<void>,

  deleteAccessToken: () => void,
  restoreSession: () => Promise<void>
}

export const AuthenticationContext = createContext<AuthenticationContextType | null>(null);

export function AuthenticationContextProvider({ children }: { children: React.ReactNode }) {
  const { setErrorMessage } = useAlertsContext();

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
      localStorage.setItem(ACCESS_TOKEN, response.accessToken);
    }

    return response;
  }

  const logoutUser = async (): Promise<void> => {
    const response = await logout(setErrorMessage);
    if (response) setAccessToken(null);
  }

  const csrfInit = async (): Promise<void> => {
    await csrf(setErrorMessage);
  }

  const restoreSession = async (): Promise<void> => {
    const response = await refresh();
    if (response) setAccessToken(response.accessToken);
  }


  const deleteAccessToken = (): void => {
    setAccessToken(null);
  }

  useEffect(() => {
    tokenRef.current = accessToken;
  }, [accessToken]);

  useEffect(() => {
    const fetchData = async (): Promise<void> => {
      await csrfInit();
      await restoreSession();
    
      setupInterceptors(
        () => tokenRef.current,
        setAccessToken,
        logoutUser
      );
    }

    fetchData();
  }, []);

  return (
    <AuthenticationContext.Provider
      value={{ accessToken, registerUser, loginUser, logoutUser, csrfInit, deleteAccessToken, restoreSession }}
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