"use client";

import { useConsentStore } from "@/stores/useConsentStore";

// academix_frontend_tdd.md §6.6 — reference component, followed closely (Tailwind classes
// kept matching the spec's reference exactly rather than this project's usual shadcn Button).
export function BiometricConsentBanner() {
  const pendingConsent = useConsentStore((state) => state.pendingConsent);
  const giveBiometricConsent = useConsentStore((state) => state.giveBiometricConsent);

  if (!pendingConsent) {
    return null;
  }

  return (
    <div className="fixed bottom-4 inset-x-4 md:inset-x-auto md:right-4 md:w-96 bg-white border border-slate-300 rounded-lg shadow-lg p-4 space-y-2">
      <h4 className="font-semibold text-sm">Yozuv biometrikasi haqida</h4>
      <p className="text-xs text-slate-600">
        {pendingConsent.studentName} ning uy vazifalarini tekshirishda, boshqasi o&apos;rniga
        yozib berishining oldini olish uchun yozuv uslubi (biometrika) tizimda saqlanadi. Bu
        ma&apos;lumot faqat shu maqsadda ishlatiladi.
      </p>
      <button
        onClick={() => giveBiometricConsent(pendingConsent.studentId)}
        className="px-3 py-1.5 bg-emerald-600 text-white text-xs rounded"
      >
        Tushundim, roziman
      </button>
    </div>
  );
}
