import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { ChildSummary, DataDeletionRequestResponse } from "@/types/parent";

interface PendingConsent {
  studentId: string;
  studentName: string;
}

interface ConsentState {
  pendingConsent: PendingConsent | null;
  checkPendingConsent: (children: ChildSummary[]) => void;
  giveBiometricConsent: (studentId: string) => Promise<void>;
  requestDataDeletion: (studentId: string) => Promise<DataDeletionRequestResponse>;
}

// academix_frontend_tdd.md §6.6 — exact named store (frontend_tdd.md's Consent State schema),
// kept separate from useParentStore since the consent banner is cross-cutting UI (shown
// wherever a parent is logged in, not scoped to one dashboard page).
export const useConsentStore = create<ConsentState>((set) => ({
  pendingConsent: null,

  // "birinchi login da tekshiriladi" (checked on first login) — no dedicated backend endpoint
  // for this, derived from the already-fetched children list: the first child whose
  // biometricConsentGiven is still false.
  checkPendingConsent: (children) => {
    const pending = children.find((c) => !c.biometricConsentGiven);
    set({ pendingConsent: pending ? { studentId: pending.studentId, studentName: pending.name } : null });
  },

  giveBiometricConsent: async (studentId) => {
    await apiClient.put(`/parent/children/${studentId}/consent/biometric`, { consentGiven: true });
    set({ pendingConsent: null });
  },

  requestDataDeletion: async (studentId) => {
    const { data } = await apiClient.post<DataDeletionRequestResponse>(
      `/parent/children/${studentId}/data-deletion-request`,
    );
    return data;
  },
}));
