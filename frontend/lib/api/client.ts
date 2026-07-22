import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import type { RefreshResponse } from "@/types/auth";
import { useAuthStore } from "@/stores/useAuthStore";

// Single shared instance — per academix_frontend_tdd.md §5.5 / CLAUDE.md. Never create a
// second Axios instance elsewhere; the 401-refresh-retry interceptor only covers calls
// made through this one.
export const apiClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1",
  withCredentials: true,
});

apiClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
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
      const refreshToken = useAuthStore.getState().refreshToken;

      try {
        // With no in-memory refresh token (fresh page load racing session bootstrap), an
        // empty body still works: the backend falls back to the httpOnly academix_refresh
        // cookie, which withCredentials carries along.
        const { data } = await axios.post<RefreshResponse>(
          `${apiClient.defaults.baseURL}/auth/refresh`,
          refreshToken ? { refreshToken } : {},
          { withCredentials: true },
        );
        useAuthStore.getState().setAccessToken(data.accessToken);
        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  },
);
