"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";
import { Button } from "@/components/ui/button";
import { useAdminStore } from "@/stores/useAdminStore";
import { useReportStore } from "@/stores/useReportStore";
import type { ReportType } from "@/types/report";

const TYPE_LABEL: Record<ReportType, string> = {
  SCHOOL: "Maktab",
  CLASS: "Sinf",
  STUDENT: "O'quvchi",
};

export default function AdminReportsPage() {
  const classes = useAdminStore((state) => state.classes);
  const students = useAdminStore((state) => state.students);
  const fetchClasses = useAdminStore((state) => state.fetchClasses);
  const fetchStudents = useAdminStore((state) => state.fetchStudents);
  const reports = useReportStore((state) => state.reports);
  const isGenerating = useReportStore((state) => state.isGenerating);
  const fetchReports = useReportStore((state) => state.fetchReports);
  const generateReport = useReportStore((state) => state.generateReport);
  const downloadReport = useReportStore((state) => state.downloadReport);

  const [type, setType] = useState<ReportType>("SCHOOL");
  const [semester, setSemester] = useState("");
  const [targetId, setTargetId] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchReports().catch(() => setError("Hisobotlar ro'yxatini yuklab bo'lmadi."));
    fetchClasses().catch(() => {});
    fetchStudents().catch(() => {});
  }, [fetchReports, fetchClasses, fetchStudents]);

  async function handleGenerate() {
    setError(null);
    if (!semester.trim()) {
      setError("Semestrni kiriting.");
      return;
    }
    if (type !== "SCHOOL" && !targetId) {
      setError("Sinf yoki o'quvchini tanlang.");
      return;
    }
    try {
      await generateReport({
        type,
        semester: semester.trim(),
        targetId: type === "SCHOOL" ? undefined : targetId,
      });
    } catch {
      setError("Hisobot yaratib bo'lmadi.");
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <AdminNav />
      <h2 className="mb-4 text-lg font-semibold">Hisobotlar</h2>

      <div className="mb-6 flex flex-wrap items-end gap-3 rounded-md border border-border p-4">
        <div>
          <label className="mb-1 block text-xs text-muted-foreground">Turi</label>
          <select
            className="rounded-md border border-border bg-background px-2 py-1.5 text-sm"
            value={type}
            onChange={(e) => {
              setType(e.target.value as ReportType);
              setTargetId("");
            }}
          >
            <option value="SCHOOL">Maktab</option>
            <option value="CLASS">Sinf</option>
            <option value="STUDENT">O&apos;quvchi</option>
          </select>
        </div>

        {type === "CLASS" ? (
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">Sinf</label>
            <select
              className="rounded-md border border-border bg-background px-2 py-1.5 text-sm"
              value={targetId}
              onChange={(e) => setTargetId(e.target.value)}
            >
              <option value="">Tanlang</option>
              {classes.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.fullName}
                </option>
              ))}
            </select>
          </div>
        ) : null}

        {type === "STUDENT" ? (
          <div>
            <label className="mb-1 block text-xs text-muted-foreground">O&apos;quvchi</label>
            <select
              className="rounded-md border border-border bg-background px-2 py-1.5 text-sm"
              value={targetId}
              onChange={(e) => setTargetId(e.target.value)}
            >
              <option value="">Tanlang</option>
              {students.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.firstName} {s.lastName}
                </option>
              ))}
            </select>
          </div>
        ) : null}

        <div>
          <label className="mb-1 block text-xs text-muted-foreground">Semestr</label>
          <input
            className="rounded-md border border-border bg-background px-2 py-1.5 text-sm"
            placeholder="2025-2026 kuz"
            value={semester}
            onChange={(e) => setSemester(e.target.value)}
          />
        </div>

        <Button size="sm" onClick={handleGenerate} disabled={isGenerating}>
          {isGenerating ? "Yaratilmoqda..." : "Hisobot yaratish"}
        </Button>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <ul className="space-y-2">
        {reports.map((r) => (
          <li
            key={r.id}
            className="flex items-center justify-between rounded-md border border-border p-3 text-sm"
          >
            <span>
              {TYPE_LABEL[r.type]} — {r.semester}
              <span className="ml-2 text-xs text-muted-foreground">
                {new Date(r.generatedAt).toLocaleString()}
              </span>
            </span>
            <Button variant="outline" size="sm" onClick={() => downloadReport(r.id)}>
              Yuklab olish
            </Button>
          </li>
        ))}
        {reports.length === 0 ? (
          <p className="text-sm text-muted-foreground">Hisobotlar yo&apos;q.</p>
        ) : null}
      </ul>
    </DashboardShell>
  );
}
