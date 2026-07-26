import axios from "axios";
import { getCsrfCookie } from "../util/api";
import { refresh } from "./auth";

export const api = axios.create({
  baseURL: "/api"
});
export const auth = axios.create({
  baseURL: "/auth",
  withCredentials: true,
});

export const ACCESS_TOKEN = "access_token";

function buildConfig(config: any, getToken: () => string | null) {
  const csrfToken = getCsrfCookie();
  const accessToken = getToken();

  if (csrfToken) config.headers["X-XSRF-TOKEN"] = csrfToken;
  if (accessToken) config.headers.Authorization = "Bearer " + accessToken;

  return config;
}

let isRefreshing = false
// While an intentional logout is in progress, a 401 is expected — don't try to
// refresh or re-trigger logout (which caused a spurious "please log in" error).
let isLoggingOut = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

export const setLoggingOut = (value: boolean) => {
  isLoggingOut = value
}

const processQueue = (error: unknown, token: string | null) => {
  failedQueue.forEach(promise => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve(token!)
    }
  })
  failedQueue = []
}

export const setupInterceptors = (
  getToken: () => string | null,
  setToken: (token: string | null) => void,
  logout: () => void
) => {
  auth.interceptors.request.use((config) => {
    return buildConfig(config, getToken);
  });

  api.interceptors.request.use((config) => {
    return buildConfig(config, getToken);
  });

  api.interceptors.response.use(
    response => response,
    async error => {
      const originalRequest = error.config

      // Only attempt the refresh→retry dance for an authenticated request that
      // hit 401. Skip it when logging out (expected) or when there was no token
      // to begin with (an anonymous request — nothing to refresh, no logout).
      const attemptRefresh =
        error.response?.status === 401 &&
        !originalRequest._retry &&
        !isLoggingOut &&
        getToken() !== null;

      if (attemptRefresh) {
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject })
          }).then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            return api(originalRequest)
          })
        }

        originalRequest._retry = true
        isRefreshing = true

        try {
          const response = await refresh();
          const newToken = response.accessToken;
          setToken(newToken)
          processQueue(null, newToken)
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return api(originalRequest)
        } catch (refreshError) {
          processQueue(refreshError, null)
          setToken(null)
          logout()
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      }

      return Promise.reject(error)
    }
  )
}