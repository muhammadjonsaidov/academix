import Image from "next/image";
import Link from "next/link";
import {
  Bell,
  Bot,
  Building2,
  Camera,
  ClipboardCheck,
  Fingerprint,
  GraduationCap,
  HeartPulse,
  LineChart,
  PenLine,
  School,
  ShieldCheck,
  Sparkles,
  Trash2,
  Users,
  Workflow,
} from "lucide-react";
import { GradingDemoLoader } from "@/components/landing/GradingDemoLoader";
import { HomeAuthRedirect } from "@/components/landing/HomeAuthRedirect";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

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

// "Kimga foyda" — the same five audiences as the old "beshta rol" grid, but framed as
// what each person GAINS (benefit headline + feature tags), not as system roles.
const AUDIENCES_BENEFIT = [
  {
    railClass: "border-l-role-teacher",
    roleClass: "text-role-teacher",
    chipClass: "bg-role-teacher-muted text-role-teacher",
    icon: ClipboardCheck,
    roleLabel: "O'qituvchi",
    title: "Vaqt — darsga qaytadi",
    text: "Daftar suratini AI o'qiydi, bosqichma-bosqich tekshiradi va baho taklif qiladi — yakuniy so'z doim sizda.",
    tags: ["Suratdan baho", "Qo'lda tuzatish", "Sinf hisoboti"],
  },
  {
    railClass: "border-l-role-student",
    roleClass: "text-role-student",
    chipClass: "bg-role-student-muted text-role-student",
    icon: Sparkles,
    roleLabel: "O'quvchi",
    title: "O'rganish — o'yinga aylanadi",
    text: "Har bir topshiriq ball, kunlik faollik va yutuq belgilari olib keladi. AI-ustoz javobni aytmaydi — yechishga yo'naltiradi.",
    tags: ["Ball va yutuqlar", "AI-ustoz", "O'z progressi"],
  },
  {
    railClass: "border-l-role-parent",
    roleClass: "text-role-parent",
    chipClass: "bg-role-parent-muted text-role-parent",
    icon: Users,
    roleLabel: "Ota-ona",
    title: "Nazar — har doim yonida",
    text: "Farzandining har bir bahosi, chorak hisoboti va muhim xabarlar Telegram orqali — bitta joyda.",
    tags: ["Baholar", "Chorak hisoboti", "Telegram xabarlar"],
  },
  {
    railClass: "border-l-role-admin",
    roleClass: "text-role-admin",
    chipClass: "bg-role-admin-muted text-role-admin",
    icon: LineChart,
    roleLabel: "Rahbariyat",
    title: "Qaror — raqamlarga asoslanadi",
    text: "Sinflar kesimida taqqoslama tahlil, chorak PDF hisobotlari va o'quvchilarni ommaviy qo'shish.",
    tags: ["Taqqoslama tahlil", "PDF hisobotlar", "Ommaviy qo'shish"],
  },
  {
    railClass: "border-l-role-psychologist",
    roleClass: "text-role-psychologist",
    chipClass: "bg-role-psychologist-muted text-role-psychologist",
    icon: HeartPulse,
    roleLabel: "Psixolog",
    title: "Yordam — o'z vaqtida keladi",
    text: "O'quvchi faoliyatidagi xavotirli o'zgarishlar avtomatik aniqlanadi va faqat mutaxassisga yetkaziladi.",
    tags: ["Avtomatik kuzatuv", "Maxfiy yetkazish", "Kuzatuv ro'yxati"],
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

const STATS = [
  { icon: Users, value: "5", label: "toifa — bitta platforma" },
  { icon: Workflow, value: "3", label: "bosqichli AI tekshiruv" },
  { icon: Bot, value: "24/7", label: "AI ustoz yonida" },
  { icon: PenLine, value: "100%", label: "qo'lyozma tahlil" },
] as const;

const HERO_POINTS = [
  { icon: Camera, text: "Suratdagi yozuvni o'qish" },
  { icon: Fingerprint, text: "Ko'chirishni aniqlash" },
  { icon: Users, text: "5 toifa uchun" },
] as const;

// The live-product marquee strip — real capabilities, repeated for a seamless loop.
const MARQUEE_ITEMS = [
  "Qo'lyozma OCR",
  "Ko'chirishni aniqlash",
  "AI baholash",
  "Shaxsiy vazifalar",
  "XP va yutuqlar",
  "Telegram xabarnomalar",
  "PDF hisobotlar",
  "Psixologik monitoring",
  "Handwriting biometriya",
  "AI ustoz",
] as const;

const TRUST = [
  {
    icon: ShieldCheck,
    chip: "bg-success-bg text-success",
    title: "Yozuv uslubi himoyasi",
    text: "Har bir o'quvchining yozuv uslubi himoyalangan holda saqlanadi va faqat ko'chirilgan ishlarni aniqlash uchun ishlatiladi.",
  },
  {
    icon: HeartPulse,
    chip: "bg-role-psychologist-muted text-role-psychologist",
    title: "Psixologik axloq",
    text: "Xavotirli o'zgarishlar faqat psixolog va o'qituvchiga boradi; ota-ona faqat eng muhim holatda xabardor qilinadi.",
  },
  {
    icon: Trash2,
    chip: "bg-destructive/10 text-destructive",
    title: "O'chirish huquqi",
    text: "Ota-ona yoki o'quvchi so'roviga ko'ra shaxsiy ma'lumotlar tizimdan to'liq o'chiriladi.",
  },
  {
    icon: Bell,
    chip: "bg-muted text-foreground",
    title: "Rozilik asosida xabarnomalar",
    text: "Telegram ulash va bildirishnomalar faqat foydalanuvchi roziligi bilan yoqiladi.",
  },
] as const;

function SectionEyebrow({ children }: { children: React.ReactNode }) {
  return (
    <p className="flex items-center gap-2 text-xs font-semibold tracking-[0.18em] text-ai uppercase">
      <span className="bg-ai-gradient h-px w-6 rounded-full" aria-hidden />
      {children}
    </p>
  );
}

// Public marketing root ("/"). Session redirects live in a small client island so the
// rest of this marketing page can stay server-rendered and out of the hydration bundle.
export default function HomePage() {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <HomeAuthRedirect />
      <header className="sticky top-0 z-40 flex items-center justify-between border-b border-border/60 bg-background/80 px-6 py-4 backdrop-blur-md lg:px-12">
        <span className="flex items-center gap-2.5">
          <Image src="/logo-mark.svg" alt="" width={32} height={29} priority />
          <span className="font-heading text-base font-semibold">
            AcademiX <span className="text-ai-gradient">AI</span>
          </span>
        </span>
        <Link href="/login" className={cn(buttonVariants({ variant: "outline" }))}>
          Tizimga kirish
        </Link>
      </header>

      <main className="flex-1">
        {/* Hero — product thesis + the live grading demo */}
        <section className="bg-grid-paper relative isolate overflow-hidden">
          {/* Ambient aurora — three drifting gradient blobs */}
          <div aria-hidden className="absolute inset-0 -z-10">
            <div
              className="aurora bg-ai-gradient absolute -top-24 right-[-6%] size-[32rem] rounded-full opacity-20 blur-3xl"
              style={{ "--aurora-delay": "0s" } as React.CSSProperties}
            />
            <div
              className="aurora absolute top-1/3 left-[-10%] size-96 rounded-full bg-chalk-green/10 blur-3xl"
              style={{ "--aurora-delay": "-5s" } as React.CSSProperties}
            />
            <div
              className="aurora absolute -bottom-32 left-1/3 size-[26rem] rounded-full bg-role-student/10 blur-3xl"
              style={{ "--aurora-delay": "-10s" } as React.CSSProperties}
            />
          </div>

          <div className="mx-auto grid max-w-6xl items-center gap-14 px-6 py-14 lg:grid-cols-2 lg:px-12 lg:py-20">
            <div className="stagger-rise flex flex-col items-start gap-6">
              <span className="inline-flex items-center gap-2 rounded-full border border-ai-soft bg-ai-soft/60 px-3 py-1 text-xs font-medium text-ai">
                <Sparkles className="size-3.5" strokeWidth={1.75} />
                AI yordamida qo&apos;lyozmani baholash
              </span>
              <h1 className="font-heading text-4xl leading-tight font-semibold tracking-tight lg:text-5xl">
                Daftardagi har bir yechim —{" "}
                <span className="text-ai-gradient">o&apos;qituvchidek</span> tekshiriladi.
              </h1>
              <p className="max-w-md text-lg text-muted-foreground">
                AcademiX AI qo&apos;lyozma uy vazifalari va nazorat ishlarini suratdan o&apos;qib,
                bosqichma-bosqich tahlil qiladi. Maktablar, xususiy maktablar va o&apos;quv markazlari
                uchun.
              </p>
              <div className="flex flex-wrap items-center gap-3">
                <Link
                  href="/login"
                  className={cn(buttonVariants({ variant: "gradient", size: "lg" }), "px-6")}
                >
                  Tizimga kirish
                </Link>
                <a
                  href="#qanday-ishlaydi"
                  className={cn(buttonVariants({ variant: "ghost", size: "lg" }))}
                >
                  Qanday ishlaydi?
                </a>
              </div>
              <ul className="flex flex-wrap items-center gap-2.5">
                {HERO_POINTS.map((p) => (
                  <li
                    key={p.text}
                    className="flex items-center gap-1.5 rounded-full border border-border bg-card px-3 py-1.5 text-xs font-medium text-muted-foreground"
                  >
                    <p.icon className="size-3.5 text-ai" strokeWidth={1.75} />
                    {p.text}
                  </li>
                ))}
              </ul>
            </div>

            {/* Live demo in a glowing glass panel with floating status chips */}
            <div className="relative">
              <div
                aria-hidden
                className="bg-ai-gradient absolute -inset-8 -z-10 rounded-[3rem] opacity-20 blur-3xl"
              />
              <div
                className="animate-rise absolute -top-4 -right-2 z-10 hidden items-center gap-1.5 rounded-full border border-ai-soft bg-card px-3 py-1.5 text-xs font-medium text-ai shadow-lg sm:flex"
                style={{ animationDelay: "300ms" }}
                aria-hidden
              >
                <Sparkles className="size-3.5" strokeWidth={1.75} />
                AI baholash
              </div>
              <div
                className="animate-rise absolute -bottom-4 -left-2 z-10 hidden items-center gap-1.5 rounded-full border border-chalk-green/30 bg-card px-3 py-1.5 text-xs font-medium text-success shadow-lg sm:flex"
                style={{ animationDelay: "520ms" }}
                aria-hidden
              >
                <ShieldCheck className="size-3.5" strokeWidth={1.75} />
                Ko&apos;chirish aniqlanadi
              </div>              <div className="gradient-ring animate-rise rounded-2xl">
                <div className="rounded-[calc(1rem-2px)] border border-ai-soft/50 bg-card/80 p-4 shadow-[0_24px_80px_-24px_var(--ai)] backdrop-blur-sm sm:p-6">
                  <p className="mb-4 text-center text-xs font-medium tracking-wide text-muted-foreground uppercase">
                    Jonli demo — AI daftarni qanday tekshiradi
                  </p>
                  <GradingDemoLoader />
                </div>
              </div>
            </div>
          </div>

          {/* Stats band — numbers count up as they enter the viewport */}
          <div className="mx-auto grid max-w-6xl grid-cols-2 gap-x-6 gap-y-8 border-t border-border px-6 py-12 sm:grid-cols-4 lg:px-12">
            {STATS.map((s) => (
              <div key={s.label} className="flex flex-col items-center gap-2 text-center">
                <span className="flex size-9 items-center justify-center rounded-lg bg-ai-soft/60 text-ai">
                  <s.icon className="size-4" strokeWidth={1.75} />
                </span>
                <span className="text-ai-gradient font-heading text-3xl font-bold">
                  {s.value}
                </span>
                <span className="text-xs text-muted-foreground">{s.label}</span>
              </div>
            ))}
          </div>
        </section>

        {/* Live-product marquee — one moving strip, reads as "in production" */}
        <div className="marquee-mask border-b border-border bg-card/60 py-4" aria-hidden>
          <div className="marquee-track">
            {[...MARQUEE_ITEMS, ...MARQUEE_ITEMS].map((item, i) => (
              <span
                key={i}
                className="flex items-center gap-2.5 pr-10 text-sm font-medium text-muted-foreground"
              >
                <span className="bg-ai-gradient size-1.5 shrink-0 rounded-full" />
                {item}
              </span>
            ))}
          </div>
        </div>

        {/* Who it's for */}
        <section className="border-y border-border bg-card/60">
          <div className="mx-auto max-w-6xl px-6 py-16 lg:px-12">
            <SectionEyebrow>Kim uchun</SectionEyebrow>
            <h2 className="font-heading mt-3 mb-8 text-2xl font-semibold">
              Har bir maktab o&apos;z darajasida
            </h2>
            <div className="grid gap-5 sm:grid-cols-3">
              {AUDIENCES.map((a, i) => (
                <div key={a.title} className="animate-rise h-full" style={{ animationDelay: `${i * 80}ms` }}>
                  <div className="card-lift hover-glow flex h-full flex-col gap-3 rounded-xl border border-border bg-card p-5">
                    <span className="flex size-10 items-center justify-center rounded-lg bg-ai-soft/60 text-ai">
                      <a.icon className="size-5" strokeWidth={1.75} />
                    </span>
                    <h3 className="font-heading text-base font-semibold">{a.title}</h3>
                    <p className="text-sm text-muted-foreground">{a.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* How it works — a real 3-step sequence, so the numbering is earned */}
        <section
          id="qanday-ishlaydi"
          className="mx-auto max-w-6xl scroll-mt-24 px-6 py-16 lg:px-12"
        >
          <SectionEyebrow>Jarayon</SectionEyebrow>
          <h2 className="font-heading mt-3 mb-8 text-2xl font-semibold">
            Qanday <span className="text-ai-gradient">ishlaydi</span>
          </h2>
          <ol className="grid gap-5 sm:grid-cols-3">
            {STEPS.map((s, i) => (
              <li key={s.title} className="animate-rise card-lift hover-glow relative flex h-full flex-col gap-3 overflow-hidden rounded-xl border border-border bg-card p-5" style={{ animationDelay: `${i * 80}ms` }}>
                <span
                  className="bg-ai-gradient absolute -top-10 -right-10 size-24 rounded-full opacity-10 blur-2xl"
                  aria-hidden
                />
                <span className="font-data text-ai-gradient text-sm font-bold">0{i + 1}</span>
                <span className="flex size-10 items-center justify-center rounded-lg bg-muted text-foreground">
                  <s.icon className="size-5" strokeWidth={1.75} />
                </span>
                <h3 className="font-heading text-base font-semibold">{s.title}</h3>
                <p className="text-sm text-muted-foreground">{s.text}</p>
              </li>
            ))}
          </ol>
        </section>

        {/* Kimga foyda — five audiences as benefit cards, wearing the dashboards' role rails */}
        <section className="mx-auto max-w-6xl px-6 pb-16 lg:px-12">
          <SectionEyebrow>Kimga foyda</SectionEyebrow>
          <h2 className="font-heading mt-3 mb-8 text-2xl font-semibold">
            Bitta platforma — <span className="text-ai-gradient">butun maktab</span>
          </h2>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {AUDIENCES_BENEFIT.map((f, i) => (
              <div key={f.roleLabel} className="animate-rise h-full" style={{ animationDelay: `${(i % 3) * 80}ms` }}>
                <div
                  className={cn(
                    "card-lift hover-glow flex h-full flex-col gap-3 rounded-xl border border-border border-l-3 bg-card p-5",
                    f.railClass,
                  )}
                >
                  <div className="flex items-center gap-3">
                    <span
                      className={cn(
                        "flex size-10 items-center justify-center rounded-lg ring-1 ring-current",
                        f.chipClass,
                      )}
                    >
                      <f.icon className="size-5" strokeWidth={1.75} />
                    </span>
                    <p
                      className={cn(
                        "text-xs font-semibold tracking-widest uppercase",
                        f.roleClass,
                      )}
                    >
                      {f.roleLabel}
                    </p>
                  </div>
                  <h3 className="font-heading text-base font-semibold">{f.title}</h3>
                  <p className="text-sm text-muted-foreground">{f.text}</p>
                  <ul className="mt-auto flex flex-wrap gap-1.5 pt-1">
                    {f.tags.map((tag) => (
                      <li
                        key={tag}
                        className="rounded-full border border-border bg-muted/50 px-2.5 py-0.5 text-xs text-muted-foreground"
                      >
                        {tag}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* Trust & privacy — the quiet differentiator */}
        <section className="border-y border-border bg-card/60">
          <div className="mx-auto max-w-6xl px-6 py-16 lg:px-12">
            <SectionEyebrow>Ishonch</SectionEyebrow>
            <h2 className="font-heading mt-3 mb-2 text-2xl font-semibold">
              Ishonch — dizaynning o&apos;zi kabi <span className="text-ai-gradient">chuqur</span>
            </h2>
            <p className="mb-8 max-w-xl text-sm text-muted-foreground">
              Nozik ma&apos;lumotlar (o&apos;quvchi yozuv uslubi, psixologik kuzatuvlar) bilan ishlaydigan
              platforma sifatida himoya va axloq birinchi o&apos;rinda.
            </p>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {TRUST.map((t, i) => (
                <div key={t.title} className="animate-rise h-full" style={{ animationDelay: `${(i % 4) * 80}ms` }}>
                  <div className="card-lift hover-glow flex h-full flex-col gap-3 rounded-xl border border-border bg-card p-5">
                    <span className={cn("flex size-10 items-center justify-center rounded-lg", t.chip)}>
                      <t.icon className="size-5" strokeWidth={1.75} />
                    </span>
                    <h3 className="font-heading text-base font-semibold">{t.title}</h3>
                    <p className="text-sm text-muted-foreground">{t.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* Closing CTA */}
        <section className="mx-auto max-w-6xl px-6 py-16 lg:px-12">
          <div className="animate-rise">
            <div className="gradient-ring rounded-2xl">
              <div className="bg-dots relative isolate overflow-hidden rounded-[calc(1rem-2px)] border border-ai-soft/40 bg-card px-6 py-16 lg:px-12">
                <div aria-hidden className="absolute inset-0 -z-10">
                  <div className="bg-ai-gradient absolute -top-24 right-[-6%] size-96 rounded-full opacity-15 blur-3xl" />
                  <div
                    className="bg-ai-gradient absolute -bottom-32 left-[-8%] size-80 rounded-full opacity-10 blur-3xl"
                    style={{ "--aurora-delay": "-6s" } as React.CSSProperties}
                  />
                </div>
                <div className="flex flex-col items-start gap-5">
                  <SectionEyebrow>Boshlash</SectionEyebrow>
                  <h2 className="font-heading max-w-xl text-2xl font-semibold">
                    O&apos;qituvchi vaqtini darsga qaytaring — tekshirishni AcademiX AI{" "}
                    <span className="text-ai-gradient">zimmasiga oling</span>.
                  </h2>
                  <Link
                    href="/login"
                    className={cn(buttonVariants({ variant: "gradient", size: "lg" }), "px-6")}
                  >
                    Tizimga kirish
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </section>
      </main>

      <footer className="border-t border-border/60 bg-card/40 px-6 py-6 text-sm text-muted-foreground lg:px-12">
        <div className="mx-auto flex max-w-6xl flex-col items-center justify-between gap-2 sm:flex-row">
          <span className="flex items-center gap-2">
            <Image src="/logo-mark.svg" alt="" width={22} height={20} />
            AcademiX AI
          </span>
          <span>Ta&apos;lim muassasalari uchun AI baholash platformasi</span>
        </div>
      </footer>
    </div>
  );
}
