import type { Metadata } from "next";

// Auth pages (login / forgot-password / reset-password) — these are client components
// so they can't export `metadata` themselves; a server-component route-group layout is
// the supported way to give them their own title (the root layout's title template
// appends "· AcademiX AI").
export const metadata: Metadata = {
  title: "Tizimga kirish",
  description:
    "AcademiX AI platformasiga telefon raqami yoki email va parol bilan kiring.",
  robots: {
    index: false,
    follow: false,
  },
};

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
