"use client";

import { useEffect } from "react";
import Link from "next/link";
import { AlertTriangle, RefreshCw } from "lucide-react";
import { Button, buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/** Product-wide recovery screen for an unexpected route rendering failure. */
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Keep diagnostics in the browser console without exposing implementation details to users.
    console.error("Unhandled route error", error);
  }, [error]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-muted/30 p-6">
      <section className="w-full max-w-md rounded-xl border border-border bg-card p-7 text-center shadow-sm">
        <span className="mx-auto flex size-12 items-center justify-center rounded-full bg-destructive/10 text-destructive">
          <AlertTriangle className="size-6" />
        </span>
        <h1 className="mt-4 font-heading text-xl font-semibold">
          Sahifani ochib bo&apos;lmadi
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Bu vaqtinchalik xatolik bo&apos;lishi mumkin. Qayta urinib
          ko&apos;ring yoki bosh sahifaga qayting.
        </p>
        <div className="mt-5 flex justify-center gap-3">
          <Button onClick={reset}>
            <RefreshCw className="size-4" /> Qayta urinish
          </Button>
          <Link href="/" className={cn(buttonVariants({ variant: "outline" }))}>
            Bosh sahifa
          </Link>
        </div>
      </section>
    </main>
  );
}
