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

export function HomeAuthRedirect() {
  const router = useRouter();
  const role = useAuthStore((state) => state.user?.role);

  useEffect(() => {
    if (role) router.replace(ROLE_DASHBOARD_PATH[role]);
  }, [role, router]);

  return null;
}
