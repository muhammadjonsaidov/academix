import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import { refreshAccessToken } from "@/lib/api/refresh";
import { useAuthStore } from "@/stores/useAuthStore";

// Single shared instance — per academix_frontend_tdd.md §5.5 / CLAUDE.md. Never create a
// second Axios instance elsewhere; the 401-refresh-retry interceptor only covers calls
// made through this one.
export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1",
  withCredentials: true,
});

apiClient.interceptors.request.use(async (config) => {
  let token = useAuthStore.getState().accessToken;
  if (!token) {
    // Right after a hard reload the in-memory token is gone and the session bootstrap is still
    // running. Wait for that same (shared) refresh instead of sending a request that is certain
    // to 401 and be retried. Not signed in at all → send it as is and let the 401 path handle it.
    try {
      token = await refreshAccessToken();
      useAuthStore.getState().setAccessToken(token);
    } catch {
      token = null;
    }
  }
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

interface RetryableConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableConfig | undefined;

    if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        // The refresh credential is only an HttpOnly cookie; JavaScript never sees it. Concurrent
        // 401s share one refresh (see refreshAccessToken) — the backend rotates the cookie.
        const accessToken = await refreshAccessToken();
        useAuthStore.getState().setAccessToken(accessToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  },
);
