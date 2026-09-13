import { create } from "zustand";
import axios from "axios";
import type { LoginResponse, Profile, UserSummary } from "@/types/auth";

interface AuthState {
  accessToken: string | null;
  user: UserSummary | null;
  profile: Profile | null;
  isAuthenticated: boolean;
  login: (phone: string, password: string) => Promise<void>;
  logout: () => void;
  setAccessToken: (token: string) => void;
  changePassword: (oldPassword: string, newPassword: string) => Promise<void>;
  forgotPassword: (phone: string) => Promise<string>;
  resetPassword: (token: string, newPassword: string) => Promise<void>;
  fetchProfile: () => Promise<void>;
  updateProfile: (firstName: string, lastName: string, email: string) => Promise<void>;
  /** Restore a session after a hard reload via the httpOnly refresh cookie. Resolves true on
   *  success. Never throws — a failed bootstrap just means "not logged in". */
  bootstrapSession: () => Promise<boolean>;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

// Deliberately plain axios here, not the shared apiClient — login/logout don't need the
// Bearer-injection or 401-refresh-retry interceptors, and importing apiClient here would
// create a real circular module dependency with lib/api/client.ts (which imports this store).
export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  user: null,
  profile: null,
  isAuthenticated: false,

  login: async (phone, password) => {
    const { data } = await axios.post<LoginResponse>(
      `${API_BASE_URL}/auth/login`,
      { phone, password },
      { withCredentials: true },
    );
    set({
      accessToken: data.accessToken,
      user: data.user,
      isAuthenticated: true,
    });
  },

  logout: () => {
    const token = get().accessToken;
    set({
      accessToken: null,
      user: null,
      profile: null,
      isAuthenticated: false,
    });
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

  changePassword: async (oldPassword, newPassword) => {
    const token = get().accessToken;
    // Deliberately plain axios (same reasoning as login/logout above), and specifically NOT
    // the shared apiClient here: a wrong current password comes back as a real 401
    // (ERR_INVALID_CREDENTIALS, same code login uses) — routing that through apiClient's
    // 401-refresh-retry interceptor would burn a refresh cycle at best, or hard-redirect to
    // /login on refresh failure at worst, neither of which is the right UX for "you typed
    // your current password wrong."
    await axios.put(
      `${API_BASE_URL}/auth/change-password`,
      { oldPassword, newPassword },
      { headers: token ? { Authorization: `Bearer ${token}` } : undefined, withCredentials: true },
    );
  },

  // Unauthenticated, same plain-axios reasoning as login/logout above. Returns the (always
  // success) message so the page can display it directly — backend intentionally never
  // reveals whether the phone/email actually matched an account (anti-enumeration).
  forgotPassword: async (phone) => {
    const { data } = await axios.post<{ success: boolean; message: string }>(
      `${API_BASE_URL}/auth/forgot-password`,
      { phone },
    );
    return data.message;
  },

  resetPassword: async (token, newPassword) => {
    await axios.post(`${API_BASE_URL}/auth/reset-password`, { token, newPassword });
  },

  // Plain axios like the rest of this store (see the module comment) — profile lives here
  // rather than a separate store since it's the same identity the header/user chrome reads.
  fetchProfile: async () => {
    const token = get().accessToken;
    const { data } = await axios.get<Profile>(`${API_BASE_URL}/auth/profile`, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      withCredentials: true,
    });
    set({ profile: data });
  },

  // Hard reload wipes the in-memory access token, but the httpOnly academix_refresh cookie
  // (path-scoped to /api/v1/auth) survives — POST /auth/refresh with no body falls back to it
  // server-side, then /auth/profile rebuilds the user summary. Plain axios like the rest of
  // this store; withCredentials carries the cookie.
  bootstrapSession: async () => {
    try {
      const { data } = await axios.post<{ accessToken: string }>(
        `${API_BASE_URL}/auth/refresh`,
        {},
        { withCredentials: true },
      );
      const profileResponse = await axios.get<Profile>(`${API_BASE_URL}/auth/profile`, {
        headers: { Authorization: `Bearer ${data.accessToken}` },
        withCredentials: true,
      });
      const profile = profileResponse.data;
      set({
        accessToken: data.accessToken,
        user: {
          id: profile.id,
          firstName: profile.firstName,
          lastName: profile.lastName,
          role: profile.role,
        },
        profile,
        isAuthenticated: true,
      });
      return true;
    } catch {
      return false;
    }
  },

  updateProfile: async (firstName, lastName, email) => {
    const token = get().accessToken;
    const { data } = await axios.put<Profile>(
      `${API_BASE_URL}/auth/profile`,
      { firstName, lastName, email },
      { headers: token ? { Authorization: `Bearer ${token}` } : undefined, withCredentials: true },
    );
    const user = get().user;
    set({
      profile: data,
      // Keep the header greeting in sync with the edited name.
      user: user ? { ...user, firstName: data.firstName, lastName: data.lastName } : user,
    });
  },
}));
