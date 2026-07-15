import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";

// Dashboard summary (GET /admin/dashboard — totals, submission rate, rankings) is later
// Sprint 1/2 scope; this page's job for now is just navigation into the CRUD sections below.
export default function AdminDashboardPage() {
  return (
    <DashboardShell role="ADMIN">
      <AdminNav />
      <p className="text-sm text-muted-foreground">
        Sinflar, o&apos;qituvchilar va o&apos;quvchilarni yuqoridagi menyudan boshqaring.
      </p>
    </DashboardShell>
  );
}
