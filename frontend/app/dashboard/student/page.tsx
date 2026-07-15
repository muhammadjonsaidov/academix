import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";

export default function StudentDashboardPage() {
  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <p className="text-sm text-muted-foreground">
        Vazifalaringiz va topshirilgan ishlaringizni yuqoridagi menyudan ko&apos;ring.
      </p>
    </DashboardShell>
  );
}
