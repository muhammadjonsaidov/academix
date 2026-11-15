import axios from "axios";
import type { RefreshResponse } from "@/types/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api/v1";

let inFlight: Promise<string> | null = null;

/**
 * Exchanges the httpOnly refresh cookie for a new access token.
 *
 * The backend rotates the refresh token on every use, so two refreshes in parallel race: the first
 * wins and the second presents an already-consumed token, gets 401 and logs the user out. That is
 * exactly what a hard reload did — the session bootstrap and every data request's 401 retry each
 * fired their own refresh. Every caller now shares the one request that is in flight.
 */
export function refreshAccessToken(): Promise<string> {
  if (!inFlight) {
    inFlight = axios
      .post<RefreshResponse>(`${API_BASE_URL}/auth/refresh`, {}, { withCredentials: true })
      .then((response) => response.data.accessToken)
      .finally(() => {
        inFlight = null;
      });
  }
  return inFlight;
}
