/**
 * Optional, separately sold product modules. The backend enforces the same switch
 * (`academix.features.*`, env `ACADEMIX_FEATURE_*`); this copy only keeps the UI from
 * showing navigation to endpoints that are not registered. Inlined at build time.
 */
export const FEATURES = {
  /** Psychological well-being monitoring + the Psychologist role (paid add-on, off by default). */
  psychology: process.env.NEXT_PUBLIC_FEATURE_PSYCHOLOGY === "true",
} as const;

/** Dashboard routes that only exist while the psychology module is on. */
export const PSYCHOLOGY_ROUTE_PREFIXES = [
  "/dashboard/psychologist",
  "/dashboard/teacher/psychological-signals",
  "/dashboard/admin/psychologists",
] as const;

export function isPsychologyRoute(pathname: string): boolean {
  return PSYCHOLOGY_ROUTE_PREFIXES.some((prefix) =>
    pathname.startsWith(prefix),
  );
}
