"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

// School settings moved into the unified settings page ("Muassasa" section on
// /dashboard/account). This route stays as a redirect so old links/bookmarks keep working.
// Redirect lives in an effect, never the render body (see DashboardShell's comment).
export default function AdminSettingsRedirect() {
  const router = useRouter();

  useEffect(() => {
    router.replace("/dashboard/account");
  }, [router]);

  return null;
}
