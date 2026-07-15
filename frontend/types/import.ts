// Hand-written, mirrors academix_tz.md §2.2 exactly — no codegen (see CLAUDE.md known gaps).

export interface ImportAnalyzeResponse {
  fileToken: string;
  detectedColumns: string[];
  suggestedMapping: Record<string, string>;
  previewRows: Record<string, string>[];
}

export interface ImportRowError {
  row: number;
  field: string;
  value: string;
  reason: string;
}

export interface ImportCommitResponse {
  totalRows: number;
  imported: number;
  failed: number;
  errors: ImportRowError[];
}

export const IMPORT_TARGET_FIELDS = [
  "firstName",
  "lastName",
  "phone",
  "classId",
  "studentNumber",
  "birthDate",
] as const;

export type ImportTargetField = (typeof IMPORT_TARGET_FIELDS)[number];
