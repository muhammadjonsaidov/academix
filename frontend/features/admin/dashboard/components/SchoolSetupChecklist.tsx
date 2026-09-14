import Link from "next/link";
import { CheckCircle2, Circle } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { AdminDashboard } from "@/types/adminAnalytics";

interface SetupStep {
  label: string;
  detail: string;
  href: string;
  complete: boolean;
}

function setupSteps(dashboard: AdminDashboard): SetupStep[] {
  return [
    {
      label: "Sinf yarating",
      detail: "O'quvchilarni joylashtirish uchun asos yarating.",
      href: "/dashboard/admin/classes",
      complete: dashboard.totalClasses > 0,
    },
    {
      label: "O'qituvchi qo'shing",
      detail: "Fanlarni olib boruvchi o'qituvchini taklif qiling.",
      href: "/dashboard/admin/teachers",
      complete: dashboard.totalTeachers > 0,
    },
    {
      label: "Fanlarni sozlang",
      detail: "Maktabda o'qitiladigan fanlarni qo'shing.",
      href: "/dashboard/admin/assignments",
      complete: dashboard.totalSubjects > 0,
    },
    {
      label: "Sinf-fan biriktirishini yarating",
      detail: "O'qituvchi, sinf va fanni bog'lang.",
      href: "/dashboard/admin/assignments",
      complete: dashboard.totalAssignments > 0,
    },
    {
      label: "O'quvchi qo'shing",
      detail: "Ro'yxat tayyor bo'lgach, o'quvchilarni qo'shing.",
      href: "/dashboard/admin/students",
      complete: dashboard.totalStudents > 0,
    },
  ];
}

/** A feature-owned, data-derived workflow: no setup progress is duplicated in client state. */
export function SchoolSetupChecklist({ dashboard }: { dashboard: AdminDashboard }) {
  const steps = setupSteps(dashboard);
  const nextStep = steps.find((step) => !step.complete);

  if (!nextStep) return null;

  const completedSteps = steps.filter((step) => step.complete).length;

  return (
    <Card className="border-ai-soft bg-ai-soft/30">
      <CardContent className="space-y-4 py-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="font-heading font-semibold">Maktabni ishga tushirish</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {completedSteps} / {steps.length} qadam bajarildi. Keyingi qadam: {nextStep.label.toLowerCase()}.
            </p>
          </div>
          <Link
            href={nextStep.href}
            className={cn(buttonVariants({ variant: "gradient" }), "shrink-0")}
          >
            Davom etish
          </Link>
        </div>
        <ol className="grid gap-2 sm:grid-cols-2 xl:grid-cols-5">
          {steps.map((step, index) => (
            <li key={step.label}>
              <Link
                href={step.href}
                className="card-lift flex h-full gap-2 rounded-md border border-border bg-background/75 p-3 text-sm hover:border-ai-soft"
              >
                {step.complete ? (
                  <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-success" />
                ) : (
                  <Circle className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                )}
                <span>
                  <span className="block font-medium">
                    {index + 1}. {step.label}
                  </span>
                  <span className="mt-0.5 block text-xs text-muted-foreground">{step.detail}</span>
                </span>
              </Link>
            </li>
          ))}
        </ol>
      </CardContent>
    </Card>
  );
}
