import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";

export default function TeacherDashboardPage() {
  return (
    <DashboardShell role="TEACHER">
      <TeacherNav />
      <p className="text-sm text-muted-foreground">
        Uy vazifalari va topshirilgan ishlarni yuqoridagi menyudan boshqaring.
      </p>
    </DashboardShell>
  );
}
