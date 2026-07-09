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
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

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

      if (error.response?.status === 401 && !originalRequest._retry) {
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