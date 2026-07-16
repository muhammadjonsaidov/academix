import Link from "next/link";

const LINKS = [
  { href: "/dashboard/psychologist", label: "Bosh sahifa" },
  { href: "/dashboard/psychologist/signals", label: "Signallar" },
  { href: "/dashboard/psychologist/watchlist", label: "Kuzatuv ro'yxati" },
  { href: "/dashboard/psychologist/reports", label: "Hisobotlar" },
];

export function PsychologistNav() {
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
