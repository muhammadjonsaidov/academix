import {
  AlertTriangle,
  BarChart3,
  Bell,
  BookOpen,
  Brain,
  ClipboardList,
  Eye,
  FileBarChart,
  FileCheck2,
  GraduationCap,
  HeartPulse,
  History,
  LayoutDashboard,
  Library,
  Link2,
  ListChecks,
  MessageSquareText,
  NotebookPen,
  School,
  Settings,
  ShieldOff,
  TrendingUp,
  Trophy,
  Users,
  type LucideIcon,
} from "lucide-react";

import type { Role } from "@/types/auth";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

export interface NavGroup {
  /** Omit for a flat (ungrouped) section — matches ui-ux-designer's guidance that only
   * TEACHER has enough items to warrant grouped section headers. */
  label?: string;
  items: NavItem[];
}

/**
 * One entry per role, built from the confirmed route list. Drill-in-only routes
 * (`/dashboard/teacher/classes/[classId]/analytics`, `/dashboard/teacher/students/[studentId]/progress`,
 * `/dashboard/parent/children/[studentId]`) are intentionally excluded — they're reached by
 * clicking through a list page, never a direct nav target.
 *
 * `/dashboard/teacher/classes` (a top-level classes *list* page) was flagged by the shell-
 * stage agent as missing and resolved by the teacher-dashboard agent: `GET /teacher/classes`
 * (academix_tz.md §2.3) was already wired via `useTeacherStore.fetchClasses`/`classes` for
 * the homework/exam/syllabus create forms, so a standalone browse page reusing that same
 * data was a real, spec-backed page rather than an invented one — it links into the existing
 * `[classId]/analytics` drill-in.
 */
export const NAV_CONFIG: Record<Role, NavGroup[]> = {
  ADMIN: [
    {
      items: [
        { href: "/dashboard/admin", label: "Bosh sahifa", icon: LayoutDashboard },
        { href: "/dashboard/admin/classes", label: "Sinflar", icon: School },
        { href: "/dashboard/admin/teachers", label: "O'qituvchilar", icon: Users },
        { href: "/dashboard/admin/students", label: "O'quvchilar", icon: GraduationCap },
        { href: "/dashboard/admin/psychologists", label: "Psixologlar", icon: Brain },
        { href: "/dashboard/admin/assignments", label: "Biriktirishlar", icon: Link2 },
        { href: "/dashboard/admin/analytics", label: "Tahlil", icon: BarChart3 },
        { href: "/dashboard/admin/reports", label: "Hisobotlar", icon: FileBarChart },
        {
          href: "/dashboard/admin/data-deletion-requests",
          label: "O'chirish so'rovlari",
          icon: ShieldOff,
        },
        { href: "/dashboard/admin/settings", label: "Sozlamalar", icon: Settings },
      ],
    },
  ],
  TEACHER: [
    {
      items: [{ href: "/dashboard/teacher", label: "Bosh sahifa", icon: LayoutDashboard }],
    },
    {
      label: "O'qitish",
      items: [
        { href: "/dashboard/teacher/homework", label: "Uy vazifalari", icon: BookOpen },
        { href: "/dashboard/teacher/exams", label: "Nazorat ishlari", icon: ClipboardList },
        {
          href: "/dashboard/teacher/grading-criteria",
          label: "Baholash mezonlari",
          icon: ListChecks,
        },
        { href: "/dashboard/teacher/syllabuses", label: "Darsliklar", icon: Library },
        { href: "/dashboard/teacher/lesson-plans", label: "Dars rejalari", icon: NotebookPen },
      ],
    },
    {
      label: "Sinf",
      items: [
        { href: "/dashboard/teacher/classes", label: "Sinflarim", icon: School },
        { href: "/dashboard/teacher/submissions", label: "Topshirilgan ishlar", icon: FileCheck2 },
        {
          href: "/dashboard/teacher/psychological-signals",
          label: "Psixologik signallar",
          icon: HeartPulse,
        },
      ],
    },
  ],
  STUDENT: [
    {
      items: [
        { href: "/dashboard/student", label: "Bosh sahifa", icon: LayoutDashboard },
        { href: "/dashboard/student/homework", label: "Vazifalar", icon: BookOpen },
        {
          href: "/dashboard/student/submissions",
          label: "Topshirilgan ishlarim",
          icon: FileCheck2,
        },
        { href: "/dashboard/student/exams", label: "Nazorat ishlarim", icon: ClipboardList },
        { href: "/dashboard/student/ai-chat", label: "AI Tutor", icon: MessageSquareText },
        { href: "/dashboard/student/progress", label: "Progress", icon: TrendingUp },
        { href: "/dashboard/student/xp-history", label: "XP tarixi", icon: History },
        { href: "/dashboard/student/badges", label: "Yutuqlar", icon: Trophy },
      ],
    },
  ],
  PARENT: [
    {
      items: [
        { href: "/dashboard/parent", label: "O'quvchilar ro'yxati", icon: Users },
      ],
    },
  ],
  PSYCHOLOGIST: [
    {
      items: [
        { href: "/dashboard/psychologist", label: "Bosh sahifa", icon: LayoutDashboard },
        { href: "/dashboard/psychologist/signals", label: "Signallar", icon: AlertTriangle },
        { href: "/dashboard/psychologist/watchlist", label: "Kuzatuv ro'yxati", icon: Eye },
        { href: "/dashboard/psychologist/reports", label: "Hisobotlar", icon: FileBarChart },
      ],
    },
  ],
};

// Kept for potential future use in a compact/notifications nav item — not wired into
// NAV_CONFIG since NotificationBell already covers this in the header (avoids duplicating
// the same affordance in two places).
export const NOTIFICATIONS_ICON = Bell;

/** Role-scoped, per-role literal Tailwind class strings — deliberately NOT built via a
 * `text-role-${role}` template string. Tailwind v4's content scanner only picks up classes
 * that appear as complete literals in source; a dynamically-interpolated class name would
 * silently never be generated. */
export const ROLE_ACCENT_CLASSES: Record<
  Role,
  { active: string; border: string; badge: "role-admin" | "role-teacher" | "role-student" | "role-parent" | "role-psychologist" }
> = {
  ADMIN: {
    active: "bg-role-admin-muted text-role-admin",
    border: "border-role-admin",
    badge: "role-admin",
  },
  TEACHER: {
    active: "bg-role-teacher-muted text-role-teacher",
    border: "border-role-teacher",
    badge: "role-teacher",
  },
  STUDENT: {
    active: "bg-role-student-muted text-role-student",
    border: "border-role-student",
    badge: "role-student",
  },
  PARENT: {
    active: "bg-role-parent-muted text-role-parent",
    border: "border-role-parent",
    badge: "role-parent",
  },
  PSYCHOLOGIST: {
    active: "bg-role-psychologist-muted text-role-psychologist",
    border: "border-role-psychologist",
    badge: "role-psychologist",
  },
};

/**
 * A nav item is active on an exact match, or when the current path is nested under it
 * (e.g. `/dashboard/teacher/homework/123/review` keeps "Uy vazifalari" highlighted). The
 * bare role-root item (`/dashboard/{role}`) is exempt from prefix matching, or it would
 * stay highlighted on every sub-route too.
 */
export function isNavItemActive(pathname: string, href: string): boolean {
  if (pathname === href) return true;
  const isRoleRoot = /^\/dashboard\/[a-z]+$/.test(href);
  if (isRoleRoot) return false;
  return pathname.startsWith(`${href}/`);
}
