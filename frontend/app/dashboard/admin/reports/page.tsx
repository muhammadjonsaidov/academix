"use client";

import { useEffect, useState } from "react";
import { Download, FileBarChart, FileText } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
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
  const [quarter, setQuarter] = useState("");
  const [targetId, setTargetId] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  useEffect(() => {
    fetchReports()
      .catch(() => setError("Hisobotlar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
    fetchClasses().catch(() => {});
    fetchStudents().catch(() => {});
  }, [fetchReports, fetchClasses, fetchStudents]);

  async function handleGenerate() {
    setError(null);
    if (!quarter.trim()) {
      setError("Chorakni kiriting.");
      return;
    }
    if (type !== "SCHOOL" && !targetId) {
      setError("Sinf yoki o'quvchini tanlang.");
      return;
    }
    try {
      await generateReport({
        type,
        quarter: quarter.trim(),
        targetId: type === "SCHOOL" ? undefined : targetId,
      });
    } catch {
      setError("Hisobot yaratib bo'lmadi.");
    }
  }

  async function handleDownload(reportId: string) {
    setDownloadingId(reportId);
    try {
      await downloadReport(reportId);
    } finally {
      setDownloadingId(null);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Hisobotlar</h2>
          <p className="text-sm text-muted-foreground">
            Maktab, sinf yoki o&apos;quvchi bo&apos;yicha choraklik PDF hisobot yarating.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <FileBarChart className="size-4" strokeWidth={1.75} />
              Yangi hisobot yaratish
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-wrap items-end gap-3">
              <FormField label="Turi" htmlFor="type">
                <select
                  id="type"
                  className={fieldClass}
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
              </FormField>

              {type === "CLASS" ? (
                <FormField label="Sinf" htmlFor="targetClass">
                  <select
                    id="targetClass"
                    className={fieldClass}
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
                </FormField>
              ) : null}

              {type === "STUDENT" ? (
                <FormField label="O'quvchi" htmlFor="targetStudent">
                  <select
                    id="targetStudent"
                    className={fieldClass}
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
                </FormField>
              ) : null}

              <FormField label="Chorak" htmlFor="quarter">
                <input
                  id="quarter"
                  className={fieldClass}
                  placeholder="2025-2026-1"
                  value={quarter}
                  onChange={(e) => setQuarter(e.target.value)}
                />
              </FormField>

              <Button onClick={handleGenerate} disabled={isGenerating}>
                {isGenerating ? "Yaratilmoqda..." : "Hisobot yaratish"}
              </Button>
            </div>
            {error ? <p className="mt-3 text-sm text-destructive">{error}</p> : null}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Yaratilgan hisobotlar</CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-2">
                <Skeleton className="h-12 w-full" />
                <Skeleton className="h-12 w-full" />
              </div>
            ) : reports.length === 0 ? (
              <EmptyState
                icon={FileText}
                title="Hisobotlar yo'q"
                description="Yuqoridagi shakl orqali birinchi hisobotni yarating."
              />
            ) : (
              <ul className="space-y-2">
                {reports.map((r) => (
                  <li
                    key={r.id}
                    className="flex items-center justify-between rounded-md border border-border px-3 py-2.5 text-sm"
                  >
                    <div className="flex items-center gap-2">
                      <Badge variant="outline">{TYPE_LABEL[r.type]}</Badge>
                      <span className="font-medium">{r.quarter}</span>
                      <span className="font-data text-xs text-muted-foreground">
                        {new Date(r.generatedAt).toLocaleString()}
                      </span>
                    </div>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={downloadingId === r.id}
                      onClick={() => handleDownload(r.id)}
                    >
                      <Download className="size-3.5" strokeWidth={1.75} />
                      {downloadingId === r.id ? "..." : "Yuklab olish"}
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardShell>
  );
}
