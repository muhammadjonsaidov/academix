import { create } from "zustand";
import { apiClient } from "@/lib/api/client";
import type { GenerateReportRequest, Report } from "@/types/report";

interface ReportState {
  reports: Report[];
  isGenerating: boolean;
  fetchReports: () => Promise<void>;
  generateReport: (request: GenerateReportRequest) => Promise<void>;
  downloadReport: (reportId: string) => Promise<void>;
}

export const useReportStore = create<ReportState>((set, get) => ({
  reports: [],
  isGenerating: false,

  fetchReports: async () => {
    const { data } = await apiClient.get<Report[]>("/admin/reports");
    set({ reports: data });
  },

  generateReport: async (request) => {
    set({ isGenerating: true });
    try {
      await apiClient.post("/admin/reports/generate", request);
      await get().fetchReports();
    } finally {
      set({ isGenerating: false });
    }
  },

  // PDF is authenticated (Authorization header via the shared apiClient), so a plain <a href>
  // won't work — fetch as a blob and trigger the browser's native download via a temp anchor.
  downloadReport: async (reportId) => {
    const response = await apiClient.get(`/admin/reports/${reportId}/download`, {
      responseType: "blob",
    });
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement("a");
    link.href = url;
    link.download = `report-${reportId}.pdf`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
}));
