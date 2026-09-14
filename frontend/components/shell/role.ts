import type { Role } from "@/types/auth";

/** Human-readable role names used by the application frame. */
export const ROLE_LABEL: Record<Role, string> = {
  ADMIN: "Administrator",
  TEACHER: "O'qituvchi",
  STUDENT: "O'quvchi",
  PARENT: "Ota-ona",
  PSYCHOLOGIST: "Psixolog",
};
