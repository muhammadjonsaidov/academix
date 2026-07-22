// academix_tz.md §2.2 "Hisobotlar".
export type ReportType = "SCHOOL" | "CLASS" | "STUDENT";

export interface Report {
  id: string;
  type: ReportType;
  quarter: string;
  targetId: string | null;
  generatedBy: string;
  generatedAt: string;
}

export interface GenerateReportRequest {
  type: ReportType;
  quarter: string;
  targetId?: string;
}
