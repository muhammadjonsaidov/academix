import Image from "next/image";
import Link from "next/link";
import { buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

// SEO: a real 404 must never be indexed — letting Google cache it turns the page into a
// soft-404 that erodes rankings for the URLs that legitimately exist (next.js already
// returns the correct HTTP 404 status code for not-found.tsx; this stops the indexing side).
export const metadata = {
  title: "Sahifa topilmadi",
  description:
    "AcademiX AI — so'ralgan sahifa topilmadi yoki ko'chirilgan. Bosh sahifaga qaytib, kerakli bo'limga o'tishingiz mumkin.",
  robots: {
    index: false,
    follow: false,
  },
};

// Branded 404 — the "red pen" marks the missing page, on a ruled sheet. Stays on-theme
// (paper/ink/pen-red) so even an error page reads as part of the product, with a clear
// way back. Server component: no client state needed here.
export default function NotFoundPage() {
  return (
    <div className="relative isolate flex min-h-screen flex-col overflow-hidden bg-background">
      {/* Ambient AI glow — same language as the landing hero */}
      <div aria-hidden className="absolute inset-0 -z-10">
        <div className="bg-ai-gradient absolute -top-32 left-1/2 size-[28rem] -translate-x-1/2 rounded-full opacity-15 blur-3xl" />
        <div className="absolute -right-20 -bottom-24 size-80 rounded-full bg-chalk-green/10 blur-3xl" />
      </div>

      <header className="flex items-center justify-between px-6 py-4 lg:px-12">
        <span className="flex items-center gap-2.5">
          <Image src="/logo-mark.svg" alt="" width={32} height={29} priority />
          <span className="font-heading text-base font-semibold">
            AcademiX <span className="text-ai-gradient">AI</span>
          </span>
        </span>
        <Link href="/" className={cn(buttonVariants({ variant: "outline", size: "sm" }))}>
          Bosh sahifa
        </Link>
      </header>

      <main className="flex flex-1 items-center justify-center px-6 py-16">
        <div className="flex w-full max-w-lg flex-col items-center text-center">
          {/* Ruled sheet with the red-pen mark */}
          <div className="notebook-sheet relative mb-8 flex h-56 w-full max-w-md items-center justify-center overflow-hidden rounded-lg border border-border shadow-lg">
            <p
              className="font-heading text-ai-gradient select-none text-[7rem] leading-none font-bold"
              aria-hidden
            >
              404
            </p>
            {/* Red-pen cross over the corner — the signature mark */}
            <span
              aria-hidden
              className="animate-stamp-in absolute top-4 right-6 font-heading rotate-12 text-6xl font-bold text-destructive"
            >
              ✗
            </span>
          </div>

          <h1 className="font-heading text-2xl font-semibold">
            Bu sahifa <span className="text-ai-gradient">topilmadi</span>
          </h1>
          <p className="mt-3 max-w-sm text-sm text-muted-foreground">
            Sahifa o&apos;chirilgan, ko&apos;chirilgan yoki manzil noto&apos;g&apos;ri kiritilgan
            bo&apos;lishi mumkin. O&apos;qituvchi ham, AI ham bu daftarda hech narsa topa olmadi.
          </p>

          <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
            <Link href="/" className={cn(buttonVariants({ variant: "gradient" }), "px-6")}>
              Bosh sahifaga qaytish
            </Link>
            <Link
              href="/login"
              className={cn(buttonVariants({ variant: "ghost", size: "lg" }))}
            >
              Tizimga kirish
            </Link>
          </div>
        </div>
      </main>

      <footer className="border-t border-border/60 px-6 py-6 text-center text-sm text-muted-foreground">
        AcademiX AI — ta&apos;lim muassasalari uchun AI baholash platformasi
      </footer>
    </div>
  );
}
