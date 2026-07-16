import Link from "next/link";

const LINKS = [
  { href: "/dashboard/teacher", label: "Bosh sahifa" },
  { href: "/dashboard/teacher/homework", label: "Uy vazifalari" },
  { href: "/dashboard/teacher/submissions", label: "Topshirilgan ishlar" },
  { href: "/dashboard/teacher/exams", label: "Nazorat ishlari" },
  { href: "/dashboard/teacher/psychological-signals", label: "Psixologik signallar" },
  { href: "/dashboard/teacher/syllabuses", label: "Darsliklar" },
  { href: "/dashboard/teacher/lesson-plans", label: "Dars rejalari" },
  { href: "/dashboard/teacher/grading-criteria", label: "Baholash mezonlari" },
];

export function TeacherNav() {
  return (
    <nav className="mb-6 flex gap-4 border-b border-border pb-3">
      {LINKS.map((link) => (
        <Link
          key={link.href}
          href={link.href}
          className="text-sm font-medium text-muted-foreground hover:text-foreground"
        >
          {link.label}
        </Link>
      ))}
    </nav>
  );
}
