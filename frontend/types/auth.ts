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
  user: UserSummary;
}

export interface RefreshResponse {
  accessToken: string;
}

// Deviation — /auth/profile has no shape in academix_tz.md §2.1 (self-service settings UI),
// mirrors backend interfaces/web/auth/ProfileResponse.
export interface Profile {
  id: string;
  firstName: string;
  lastName: string;
  phone: string;
  email: string | null;
  role: Role;
}

export interface ApiErrorResponse {
  status: number;
  code: string;
  message: string;
  mitigation: string;
}
