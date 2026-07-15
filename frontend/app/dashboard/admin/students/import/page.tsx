"use client";

import { useRef } from "react";
import Link from "next/link";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";
import { Button } from "@/components/ui/button";
import { useImportWizardStore } from "@/stores/useImportWizardStore";
import { IMPORT_TARGET_FIELDS, type ImportTargetField } from "@/types/import";

const FIELD_LABEL: Record<ImportTargetField, string> = {
  firstName: "Ism",
  lastName: "Familiya",
  phone: "Telefon raqam",
  classId: "Sinf",
  studentNumber: "O'quvchi raqami",
  birthDate: "Tug'ilgan sana",
};

export default function AdminStudentsImportPage() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const {
    step,
    detectedColumns,
    columnMapping,
    previewRows,
    saveMappingAsTemplate,
    commitResult,
    isLoading,
    error,
    analyze,
    setMapping,
    setSaveMappingAsTemplate,
    goToPreview,
    commit,
    reset,
  } = useImportWizardStore();

  function handleFileChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (file) {
      analyze(file);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <AdminNav />
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-lg font-semibold">O&apos;quvchilarni Excel orqali import qilish</h2>
        <Link href="/dashboard/admin/students" className="text-sm text-muted-foreground hover:text-foreground">
          Ortga
        </Link>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      {step === "upload" ? (
        <div className="space-y-3">
          <p className="text-sm text-muted-foreground">
            Har maktabning Excel fayli boshqacha ustunlarga ega bo&apos;lishi mumkin — qattiq
            shablon talab qilinmaydi. Faylni yuklang, keyingi bosqichda ustunlarni
            moslashtirasiz.
          </p>
          <input
            ref={fileInputRef}
            type="file"
            accept=".xlsx"
            onChange={handleFileChange}
            disabled={isLoading}
            className="text-sm"
          />
          {isLoading ? <p className="text-sm text-muted-foreground">Tahlil qilinmoqda...</p> : null}
        </div>
      ) : null}

      {step === "mapping" ? (
        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Har bir maydon uchun faylingizdagi mos ustunni tanlang. Taklif qilingan moslik
            avtomatik belgilangan — tasdiqlashdan oldin tekshirib chiqing.
          </p>
          <table className="w-full max-w-lg text-left text-sm">
            <tbody>
              {IMPORT_TARGET_FIELDS.map((field) => (
                <tr key={field} className="border-b border-border">
                  <td className="py-2 pr-4 font-medium">{FIELD_LABEL[field]}</td>
                  <td className="py-2">
                    <select
                      value={columnMapping[field] ?? ""}
                      onChange={(e) => setMapping(field, e.target.value)}
                      className="rounded-md border border-input bg-background px-3 py-2 text-sm"
                    >
                      <option value="">— tanlanmagan —</option>
                      {detectedColumns.map((column) => (
                        <option key={column} value={column}>
                          {column}
                        </option>
                      ))}
                    </select>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Button onClick={goToPreview}>Ko&apos;rib chiqish</Button>
        </div>
      ) : null}

      {step === "preview" ? (
        <div className="space-y-4">
          <p className="text-sm text-muted-foreground">
            Birinchi {previewRows.length} qator, tanlangan moslik qo&apos;llangan holda:
          </p>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  {IMPORT_TARGET_FIELDS.map((field) => (
                    <th key={field} className="py-2 pr-4">
                      {FIELD_LABEL[field]}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {previewRows.map((row, index) => (
                  <tr key={index} className="border-b border-border">
                    {IMPORT_TARGET_FIELDS.map((field) => (
                      <td key={field} className="py-2 pr-4">
                        {row[field] ?? ""}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={saveMappingAsTemplate}
              onChange={(e) => setSaveMappingAsTemplate(e.target.checked)}
            />
            Ushbu moslikni keyingi importlar uchun shablon sifatida saqlash
          </label>

          <Button onClick={commit} disabled={isLoading}>
            {isLoading ? "Import qilinmoqda..." : "Importni yakunlash"}
          </Button>
        </div>
      ) : null}

      {step === "result" && commitResult ? (
        <div className="space-y-4">
          <p className="text-sm">
            Jami: {commitResult.totalRows} qator — <strong>{commitResult.imported}</strong>{" "}
            muvaffaqiyatli, <strong>{commitResult.failed}</strong> xato.
          </p>

          {commitResult.errors.length > 0 ? (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground">
                <tr className="border-b border-border">
                  <th className="py-2 pr-4">Qator</th>
                  <th className="py-2 pr-4">Maydon</th>
                  <th className="py-2 pr-4">Qiymat</th>
                  <th className="py-2">Sabab</th>
                </tr>
              </thead>
              <tbody>
                {commitResult.errors.map((error, index) => (
                  <tr key={index} className="border-b border-border">
                    <td className="py-2 pr-4">{error.row + 1}</td>
                    <td className="py-2 pr-4">{error.field}</td>
                    <td className="py-2 pr-4">{error.value}</td>
                    <td className="py-2">{error.reason}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : null}

          <Button variant="outline" onClick={reset}>
            Yana bir fayl import qilish
          </Button>
        </div>
      ) : null}
    </DashboardShell>
  );
}
