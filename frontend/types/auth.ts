// Hand-written, mirrors academix_tz.md §2.1 exactly — no codegen (see CLAUDE.md known gaps).
// Checked against the backend by the frontend-contract-auditor agent.

export type Role = "ADMIN" | "TEACHER" | "STUDENT" | "PARENT" | "PSYCHOLOGIST";

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  role: Role;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserSummary;
}

export interface RefreshResponse {
  accessToken: string;
}

export interface ApiErrorResponse {
  status: number;
  code: string;
  message: string;
  mitigation: string;
}
