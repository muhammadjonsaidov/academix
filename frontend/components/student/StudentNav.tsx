import Link from "next/link";

const LINKS = [
  { href: "/dashboard/student", label: "Bosh sahifa" },
  { href: "/dashboard/student/homework", label: "Vazifalar" },
  { href: "/dashboard/student/submissions", label: "Topshirilgan ishlarim" },
  { href: "/dashboard/student/badges", label: "Yutuqlar" },
  { href: "/dashboard/student/xp-history", label: "XP tarixi" },
];

export function StudentNav() {
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
