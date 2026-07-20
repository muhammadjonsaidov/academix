"use client";

import { Fingerprint } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useConsentStore } from "@/stores/useConsentStore";

// academix_frontend_tdd.md §6.6 — reference component, positioning/behavior kept close to
// spec (fixed bottom banner). Copy fixed per CLAUDE.md's biometric-consent accuracy rule:
// handwriting-style collection is functionally necessary for plagiarism defense and already
// happens on every submission regardless of this banner — consent here only gates
// *disclosure to the parent*, never collection. The action must always read as granting a
// view permission, never a collection permission.
export function BiometricConsentBanner() {
  const pendingConsent = useConsentStore((state) => state.pendingConsent);
  const giveBiometricConsent = useConsentStore((state) => state.giveBiometricConsent);

  if (!pendingConsent) {
    return null;
  }

  return (
    <div className="fixed inset-x-4 bottom-4 z-40 space-y-3 rounded-lg border border-border bg-card p-4 shadow-lg md:inset-x-auto md:right-4 md:w-96">
      <div className="flex items-start gap-2.5">
        <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-role-parent-muted text-role-parent">
          <Fingerprint className="size-4" strokeWidth={1.75} />
        </span>
        <h4 className="pt-1.5 text-sm font-semibold">Yozuv biometrikasi haqida</h4>
      </div>

      <p className="text-xs text-muted-foreground">
        {pendingConsent.studentName} ning uy vazifalarini tekshirishda, boshqasi o&apos;rniga
        yozib berishining oldini olish uchun yozuv uslubi (biometrika) tizimda saqlanadi. Bu
        ma&apos;lumot faqat shu maqsadda ishlatiladi.
      </p>
      <p className="text-xs text-muted-foreground">
        Yozuv uslubi ma&apos;lumotlari farzandingizning barcha uy vazifalarida allaqachon
        yig&apos;ilmoqda — bu tizim xavfsizligi (plagiat aniqlash) uchun zarur. Ushbu tugma
        faqat sizga bu ma&apos;lumotni ko&apos;rsatish huquqini beradi.
      </p>

      <Button
        type="button"
        size="sm"
        className="w-full"
        onClick={() => giveBiometricConsent(pendingConsent.studentId)}
      >
        Ko&apos;rish huquqini berish
      </Button>
    </div>
  );
}
