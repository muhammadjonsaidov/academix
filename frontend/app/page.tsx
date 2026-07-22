"use client";

import { useEffect } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  Bot,
  Building2,
  Camera,
  ClipboardCheck,
  GraduationCap,
  HeartPulse,
  LineChart,
  School,
  Sparkles,
  Users,
} from "lucide-react";
import { buttonVariants } from "@/components/ui/button";
import { InkMark } from "@/components/ui/ink-mark";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/stores/useAuthStore";
import type { Role } from "@/types/auth";

const ROLE_DASHBOARD_PATH: Record<Role, string> = {
  ADMIN: "/dashboard/admin",
  TEACHER: "/dashboard/teacher",
  STUDENT: "/dashboard/student",
  PARENT: "/dashboard/parent",
  PSYCHOLOGIST: "/dashboard/psychologist",
};

const AUDIENCES = [
  {
    icon: School,
    title: "Davlat maktablari",
    text: "Sinflar, choraklar va nazorat ishlari — mavjud o'quv tartibiga mos, qo'shimcha yuk siz.",
  },
  {
    icon: Building2,
    title: "Xususiy maktablar",
    text: "Ota-onalarga shaffof hisobot: har bir baho ortida ko'rinadigan tahlil turadi.",
  },
  {
    icon: GraduationCap,
    title: "O'quv markazlari",
    text: "Katta oqimda ham har bir o'quvchining ishini alohida tekshirish endi vaqt talab qilmaydi.",
  },
] as const;

const ROLE_FEATURES = [
  {
    railClass: "border-l-role-teacher",
    icon: ClipboardCheck,
    title: "O'qituvchiga",
    text: "Daftar suratini AI o'qiydi, bosqichma-bosqich tekshiradi va baho taklif qiladi — siz faqat tasdiqlaysiz yoki tuzatasiz.",
  },
  {
    railClass: "border-l-role-student",
    icon: Sparkles,
    title: "O'quvchiga",
    text: "Har bir topshiriq uchun XP, kunlik streak va yutuq belgilari. AI-ustoz javobni aytib bermaydi — yechishga yo'naltiradi.",
  },
  {
    railClass: "border-l-role-parent",
    icon: Users,
    title: "Ota-onaga",
    text: "Farzandining har bir bahosi, chorak hisoboti va Telegram orqali muhim xabarlar — bir joyda.",
  },
  {
    railClass: "border-l-role-admin",
    icon: LineChart,
    title: "Rahbariyatga",
    text: "Sinflar kesimida taqqoslama tahlil, chorak PDF hisobotlari va o'quvchilarni ommaviy import qilish.",
  },
  {
    railClass: "border-l-role-psychologist",
    icon: HeartPulse,
    title: "Psixologga",
    text: "O'quvchi faoliyatidagi xavotirli o'zgarishlar avtomatik aniqlanadi va faqat mutaxassisga yetkaziladi.",
  },
] as const;

const STEPS = [
  {
    icon: Camera,
    title: "Suratga oling",
    text: "O'quvchi daftardagi yechimini telefon kamerasida suratga olib topshiradi.",
  },
  {
    icon: Bot,
    title: "AI tekshiradi",
    text: "Qo'lyozma o'qiladi, har bir qadam tahlil qilinadi, ko'chirilganlik ham tekshiriladi.",
  },
  {
    icon: ClipboardCheck,
    title: "O'qituvchi tasdiqlaydi",
    text: "Tayyor tahlil va taklif etilgan baho o'qituvchiga tushadi — yakuniy so'z doim insonda.",
  },
] as const;

// Public marketing root ("/"). If a session already exists in memory, bounce to that
// role's dashboard — in an effect, never in the render body (see DashboardShell).
export default function HomePage() {
  const router = useRouter();
  const user = useAuthStore((state) => state.user);

  useEffect(() => {
    if (user) {
      router.replace(ROLE_DASHBOARD_PATH[user.role]);
    }
  }, [user, router]);

  if (user) {
    return null;
  }

  return (
    <div className="flex min-h-screen flex-col bg-background">
      <header className="flex items-center justify-between px-6 py-4 lg:px-12">
        <span className="flex items-center gap-2.5">
          <span className="flex size-8 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground">
            A
          </span>
          <span className="font-heading text-base font-semibold">AcademiX AI</span>
        </span>
        <Link href="/login" className={cn(buttonVariants({ variant: "outline" }))}>
          Tizimga kirish
        </Link>
      </header>

      <main className="flex-1">
        {/* Hero: the product thesis acted out on an exercise-book sheet */}
        <section className="mx-auto grid max-w-6xl items-center gap-12 px-6 py-16 lg:grid-cols-2 lg:px-12 lg:py-24">
          <div className="stagger-rise flex flex-col items-start gap-6">
            <h1 className="font-heading text-4xl leading-tight font-semibold tracking-tight lg:text-5xl">
              Daftardagi har bir yechim — o&apos;qituvchidek tekshiriladi.
            </h1>
            <p className="max-w-md text-lg text-muted-foreground">
              AcademiX AI qo&apos;lyozma uy vazifalari va nazorat ishlarini suratdan o&apos;qib,
              bosqichma-bosqich tahlil qiladi. Maktablar, xususiy maktablar va o&apos;quv markazlari
              uchun.
            </p>
            <div className="flex flex-wrap items-center gap-3">
              <Link href="/login" className={cn(buttonVariants({ size: "lg" }), "px-6")}>
                Tizimga kirish
              </Link>
              <a
                href="#qanday-ishlaydi"
                className={cn(buttonVariants({ variant: "ghost", size: "lg" }))}
              >
                Qanday ishlaydi?
              </a>
            </div>
          </div>

          {/* The sheet: ruled paper, red margin, an answer being marked live */}
          <div
            className="notebook-sheet animate-rise relative mx-auto min-h-80 w-full max-w-md -rotate-1 rounded-sm border border-border py-8 pr-6 pl-14 shadow-md"
            style={{ animationDelay: "150ms" }}
            aria-hidden
          >
            <p className="text-sm leading-8 text-muted-foreground">
              Misol: 24 × 3 + 18 = ?
            </p>
            <p
              className="animate-rise font-heading text-xl leading-8 font-semibold text-ink"
              style={{ animationDelay: "500ms" }}
            >
              24 × 3 = 72; &nbsp;72 + 18 = 90{" "}
              <InkMark
                variant="check"
                draw
                className="ml-1 size-6"
                style={{ "--ink-draw-delay": "1s" } as React.CSSProperties}
              />
            </p>
            <p
              className="animate-rise text-sm leading-8 text-pen-red"
              style={{ animationDelay: "1.5s" }}
            >
              Barakalla — ikkala qadam ham to&apos;g&apos;ri.
            </p>
            <span
              className="animate-stamp-in font-heading absolute top-5 right-5 flex size-14 items-center justify-center rounded-[50%_48%_52%_49%/48%_52%_49%_51%] border-2 border-pen-red text-2xl font-bold text-pen-red"
              style={{ animationDelay: "1.9s" }}
            >
              5
            </span>
          </div>
        </section>

        {/* Who it's for */}
        <section className="border-y border-border bg-card/60">
          <div className="mx-auto grid max-w-6xl gap-6 px-6 py-12 sm:grid-cols-3 lg:px-12">
            {AUDIENCES.map((a) => (
              <div key={a.title} className="flex flex-col gap-2">
                <a.icon className="size-5 text-muted-foreground" strokeWidth={1.75} />
                <h2 className="font-heading text-base font-semibold">{a.title}</h2>
                <p className="text-sm text-muted-foreground">{a.text}</p>
              </div>
            ))}
          </div>
        </section>

        {/* How it works — a real 3-step sequence, so the numbering is earned */}
        <section id="qanday-ishlaydi" className="mx-auto max-w-6xl px-6 py-16 lg:px-12">
          <h2 className="font-heading mb-8 text-2xl font-semibold">Qanday ishlaydi</h2>
          <ol className="grid gap-6 sm:grid-cols-3">
            {STEPS.map((s, i) => (
              <li
                key={s.title}
                className="card-lift flex flex-col gap-3 rounded-lg border border-border bg-card p-5"
              >
                <span className="flex items-center gap-2.5">
                  <span className="font-data text-sm text-muted-foreground">0{i + 1}</span>
                  <s.icon className="size-5 text-ink" strokeWidth={1.75} />
                </span>
                <h3 className="font-heading text-base font-semibold">{s.title}</h3>
                <p className="text-sm text-muted-foreground">{s.text}</p>
              </li>
            ))}
          </ol>
        </section>

        {/* Five roles, five rails — the same role accents the dashboards wear */}
        <section className="mx-auto max-w-6xl px-6 pb-16 lg:px-12">
          <h2 className="font-heading mb-8 text-2xl font-semibold">
            Bitta platforma — beshta rol
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {ROLE_FEATURES.map((f) => (
              <div
                key={f.title}
                className={cn(
                  "card-lift flex flex-col gap-2 rounded-lg border border-border border-l-3 bg-card p-5",
                  f.railClass,
                )}
              >
                <span className="flex items-center gap-2">
                  <f.icon className="size-4.5 text-muted-foreground" strokeWidth={1.75} />
                  <h3 className="font-heading text-base font-semibold">{f.title}</h3>
                </span>
                <p className="text-sm text-muted-foreground">{f.text}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Closing CTA */}
        <section className="border-t border-border bg-card/60">
          <div className="mx-auto flex max-w-6xl flex-col items-start gap-4 px-6 py-14 lg:px-12">
            <h2 className="font-heading max-w-xl text-2xl font-semibold">
              O&apos;qituvchi vaqtini darsga qaytaring — tekshirishni AcademiX AI zimmasiga oling.
            </h2>
            <Link href="/login" className={cn(buttonVariants({ size: "lg" }), "px-6")}>
              Tizimga kirish
            </Link>
          </div>
        </section>
      </main>

      <footer className="flex items-center justify-between px-6 py-6 text-sm text-muted-foreground lg:px-12">
        <span>AcademiX AI</span>
        <span>Ta&apos;lim muassasalari uchun AI baholash platformasi</span>
      </footer>
    </div>
  );
}
