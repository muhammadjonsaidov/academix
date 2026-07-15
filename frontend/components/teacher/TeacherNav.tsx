import Link from "next/link";

const LINKS = [
  { href: "/dashboard/teacher", label: "Bosh sahifa" },
  { href: "/dashboard/teacher/homework", label: "Uy vazifalari" },
  { href: "/dashboard/teacher/submissions", label: "Topshirilgan ishlar" },
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
