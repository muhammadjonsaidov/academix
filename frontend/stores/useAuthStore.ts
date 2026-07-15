import { create } from "zustand";
import axios from "axios";
import type { LoginResponse, UserSummary } from "@/types/auth";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserSummary | null;
  isAuthenticated: boolean;
  login: (phone: string, password: string) => Promise<void>;
  logout: () => void;
  setAccessToken: (token: string) => void;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

// Deliberately plain axios here, not the shared apiClient — login/logout don't need the
// Bearer-injection or 401-refresh-retry interceptors, and importing apiClient here would
// create a real circular module dependency with lib/api/client.ts (which imports this store).
export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false,

  login: async (phone, password) => {
    const { data } = await axios.post<LoginResponse>(
      `${API_BASE_URL}/auth/login`,
      { phone, password },
      { withCredentials: true },
    );
    set({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      user: data.user,
      isAuthenticated: true,
    });
  },

  logout: () => {
    const token = get().accessToken;
    set({ accessToken: null, refreshToken: null, user: null, isAuthenticated: false });
    if (token) {
      // Best-effort — the client-side session is already cleared above regardless of
      // whether this call succeeds, matching the store's job (client state), not the
      // server's (revoking the refresh token server-side).
      axios
        .post(
          `${API_BASE_URL}/auth/logout`,
          {},
          { headers: { Authorization: `Bearer ${token}` }, withCredentials: true },
        )
        .catch(() => {});
    }
  },

  setAccessToken: (token) => set({ accessToken: token }),
}));
