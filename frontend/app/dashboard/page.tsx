"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/useAuthStore";
import type { Role } from "@/types/auth";

const ROLE_DASHBOARD_PATH: Record<Role, string> = {
  ADMIN: "/dashboard/admin",
  TEACHER: "/dashboard/teacher",
  STUDENT: "/dashboard/student",
  PARENT: "/dashboard/parent",
  PSYCHOLOGIST: "/dashboard/psychologist",
};

// proxy.ts redirects wrong-role access here (bare /dashboard, not a specific role path) —
// this page's only job is bouncing the user to where they actually belong.
export default function DashboardIndexPage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);

  useEffect(() => {
    router.replace(user ? ROLE_DASHBOARD_PATH[user.role] : "/login");
  }, [user, router]);

  return null;
}
