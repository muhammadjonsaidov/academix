import Link from "next/link";

const LINKS = [
  { href: "/dashboard/admin", label: "Bosh sahifa" },
  { href: "/dashboard/admin/classes", label: "Sinflar" },
  { href: "/dashboard/admin/teachers", label: "O'qituvchilar" },
  { href: "/dashboard/admin/students", label: "O'quvchilar" },
  { href: "/dashboard/admin/analytics", label: "Tahlil" },
  { href: "/dashboard/admin/reports", label: "Hisobotlar" },
];

export function AdminNav() {
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
