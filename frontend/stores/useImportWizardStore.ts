import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { ImportAnalyzeResponse, ImportCommitResponse } from "@/types/import";

export type ImportWizardStep = "upload" | "mapping" | "preview" | "result";

interface ImportWizardState {
  step: ImportWizardStep;
  fileToken: string | null;
  detectedColumns: string[];
  columnMapping: Record<string, string>;
  previewRows: Record<string, string>[];
  saveMappingAsTemplate: boolean;
  commitResult: ImportCommitResponse | null;
  isLoading: boolean;
  error: string | null;

  analyze: (file: File) => Promise<void>;
  setMapping: (field: string, header: string) => void;
  setSaveMappingAsTemplate: (value: boolean) => void;
  goToPreview: () => void;
  commit: () => Promise<void>;
  reset: () => void;
}

const INITIAL_STATE = {
  step: "upload" as ImportWizardStep,
  fileToken: null as string | null,
  detectedColumns: [] as string[],
  columnMapping: {} as Record<string, string>,
  previewRows: [] as Record<string, string>[],
  saveMappingAsTemplate: false,
  commitResult: null as ImportCommitResponse | null,
  isLoading: false,
  error: null as string | null,
};

// Store spans the upload -> mapping -> preview -> commit steps (per add-import-wizard-view
// skill: fileToken lives here, not component-local state, since the wizard spans components).
export const useImportWizardStore = create<ImportWizardState>((set, get) => ({
  ...INITIAL_STATE,

  analyze: async (file) => {
    set({ isLoading: true, error: null });
    try {
      const formData = new FormData();
      formData.append("file", file);
      const { data } = await apiClient.post<ImportAnalyzeResponse>(
        "/admin/students/bulk-import/analyze",
        formData,
        { headers: { "Content-Type": "multipart/form-data" } },
      );
      set({
        step: "mapping",
        fileToken: data.fileToken,
        detectedColumns: data.detectedColumns,
        columnMapping: data.suggestedMapping,
        previewRows: data.previewRows,
        isLoading: false,
      });
    } catch {
      set({ isLoading: false, error: "Faylni tahlil qilib bo'lmadi." });
    }
  },

  setMapping: (field, header) => {
    set({ columnMapping: { ...get().columnMapping, [field]: header } });
  },

  setSaveMappingAsTemplate: (value) => set({ saveMappingAsTemplate: value }),

  goToPreview: () => set({ step: "preview" }),

  commit: async () => {
    const { fileToken, columnMapping, saveMappingAsTemplate } = get();
    if (!fileToken) return;
    set({ isLoading: true, error: null });
    try {
      const { data } = await apiClient.post<ImportCommitResponse>(
        "/admin/students/bulk-import/commit",
        { fileToken, columnMapping, saveMappingAsTemplate },
      );
      set({ step: "result", commitResult: data, isLoading: false });
    } catch {
      set({ isLoading: false, error: "Import yakunlanmadi." });
    }
  },

  reset: () => set({ ...INITIAL_STATE }),
}));
