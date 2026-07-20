"use client";

import { useRef } from "react";
import Link from "next/link";
import { ArrowLeft, CheckCircle2, FileSpreadsheet, ListChecks, Table2, Upload } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { fieldClass } from "@/components/admin/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription, CardFooter } from "@/components/ui/card";
import { Spinner } from "@/components/ui/spinner";
import { useImportWizardStore, type ImportWizardStep } from "@/stores/useImportWizardStore";
import { IMPORT_TARGET_FIELDS, type ImportTargetField } from "@/types/import";
import { cn } from "@/lib/utils";

const FIELD_LABEL: Record<ImportTargetField, string> = {
  firstName: "Ism",
  lastName: "Familiya",
  phone: "Telefon raqam",
  classId: "Sinf",
  studentNumber: "O'quvchi raqami",
  birthDate: "Tug'ilgan sana",
};

const STEPS: { key: ImportWizardStep; label: string; icon: typeof Upload }[] = [
  { key: "upload", label: "Yuklash", icon: Upload },
  { key: "mapping", label: "Moslashtirish", icon: ListChecks },
  { key: "preview", label: "Ko'rib chiqish", icon: Table2 },
  { key: "result", label: "Natija", icon: CheckCircle2 },
];

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

  const currentStepIndex = STEPS.findIndex((s) => s.key === step);

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-heading text-xl font-semibold">
              O&apos;quvchilarni Excel orqali import qilish
            </h2>
            <p className="text-sm text-muted-foreground">
              Ko&apos;p bosqichli jarayon — har bir bosqichni tasdiqlagandan so&apos;ng keyingisiga o&apos;tasiz.
            </p>
          </div>
          <Button variant="outline" size="sm" render={<Link href="/dashboard/admin/students" />}>
            <ArrowLeft className="size-3.5" strokeWidth={1.75} />
            Ortga
          </Button>
        </div>

        {/* Step indicator */}
        <div className="flex flex-wrap items-center gap-2">
          {STEPS.map((s, index) => {
            const isDone = index < currentStepIndex;
            const isCurrent = index === currentStepIndex;
            return (
              <Badge
                key={s.key}
                variant={isCurrent ? "default" : isDone ? "success" : "secondary"}
                className={cn(!isCurrent && !isDone && "opacity-60")}
              >
                <s.icon className="size-3" strokeWidth={1.75} />
                {index + 1}. {s.label}
              </Badge>
            );
          })}
        </div>

        {error ? (
          <Card className="border-destructive/30">
            <CardContent className="py-4 text-sm text-destructive">{error}</CardContent>
          </Card>
        ) : null}

        {step === "upload" ? (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <FileSpreadsheet className="size-4" strokeWidth={1.75} />
                1. Faylni yuklash
              </CardTitle>
              <CardDescription>
                Har maktabning Excel fayli boshqacha ustunlarga ega bo&apos;lishi mumkin —
                qattiq shablon talab qilinmaydi. Faylni yuklang, keyingi bosqichda ustunlarni
                moslashtirasiz.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx"
                onChange={handleFileChange}
                disabled={isLoading}
                className="text-sm file:mr-3 file:rounded-md file:border file:border-input file:bg-background file:px-3 file:py-1.5 file:text-sm file:font-medium"
              />
              {isLoading ? <Spinner label="Tahlil qilinmoqda" className="text-sm text-muted-foreground gap-2" /> : null}
            </CardContent>
          </Card>
        ) : null}

        {step === "mapping" ? (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ListChecks className="size-4" strokeWidth={1.75} />
                2. Ustunlarni moslashtirish
              </CardTitle>
              <CardDescription>
                Har bir maydon uchun faylingizdagi mos ustunni tanlang. Taklif qilingan moslik
                avtomatik belgilangan — tasdiqlashdan oldin tekshirib chiqing.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <table className="w-full max-w-lg text-left text-sm">
                <tbody>
                  {IMPORT_TARGET_FIELDS.map((field) => (
                    <tr key={field} className="border-b border-border last:border-0">
                      <td className="py-2 pr-4 font-medium">{FIELD_LABEL[field]}</td>
                      <td className="py-2">
                        <select
                          value={columnMapping[field] ?? ""}
                          onChange={(e) => setMapping(field, e.target.value)}
                          className={fieldClass}
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
            </CardContent>
            <CardFooter>
              <Button onClick={goToPreview}>Ko&apos;rib chiqish</Button>
            </CardFooter>
          </Card>
        ) : null}

        {step === "preview" ? (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Table2 className="size-4" strokeWidth={1.75} />
                3. Ko&apos;rib chiqish
              </CardTitle>
              <CardDescription>
                Birinchi {previewRows.length} qator, tanlangan moslik qo&apos;llangan holda:
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="overflow-x-auto rounded-md border border-border">
                <table className="w-full text-left text-sm">
                  <thead className="text-muted-foreground">
                    <tr className="border-b border-border">
                      {IMPORT_TARGET_FIELDS.map((field) => (
                        <th key={field} className="px-3 py-2 font-medium">
                          {FIELD_LABEL[field]}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {previewRows.map((row, index) => (
                      <tr key={index} className="border-b border-border last:border-0">
                        {IMPORT_TARGET_FIELDS.map((field) => (
                          <td key={field} className="px-3 py-2 font-data">
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
            </CardContent>
            <CardFooter>
              <Button onClick={commit} disabled={isLoading}>
                {isLoading ? <Spinner label="Import qilinmoqda" className="gap-2 text-primary-foreground" /> : null}
                {isLoading ? "Import qilinmoqda..." : "Importni yakunlash"}
              </Button>
            </CardFooter>
          </Card>
        ) : null}

        {step === "result" && commitResult ? (
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <CheckCircle2 className="size-4" strokeWidth={1.75} />
                4. Natija
              </CardTitle>
              <CardDescription>
                Jami: <span className="font-data">{commitResult.totalRows}</span> qator —{" "}
                <Badge variant="success" className="mx-1">
                  {commitResult.imported} muvaffaqiyatli
                </Badge>
                {commitResult.failed > 0 ? (
                  <Badge variant="destructive" className="mx-1">
                    {commitResult.failed} xato
                  </Badge>
                ) : null}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {commitResult.errors.length > 0 ? (
                <div className="overflow-x-auto rounded-md border border-border">
                  <table className="w-full text-left text-sm">
                    <thead className="text-muted-foreground">
                      <tr className="border-b border-border">
                        <th className="px-3 py-2 font-medium">Qator</th>
                        <th className="px-3 py-2 font-medium">Maydon</th>
                        <th className="px-3 py-2 font-medium">Qiymat</th>
                        <th className="px-3 py-2 font-medium">Sabab</th>
                      </tr>
                    </thead>
                    <tbody>
                      {commitResult.errors.map((rowError, index) => (
                        <tr key={index} className="border-b border-border last:border-0">
                          <td className="px-3 py-2 font-data">{rowError.row + 1}</td>
                          <td className="px-3 py-2">{rowError.field}</td>
                          <td className="px-3 py-2 font-data">{rowError.value}</td>
                          <td className="px-3 py-2 text-destructive">{rowError.reason}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </CardContent>
            <CardFooter>
              <Button variant="outline" onClick={reset}>
                Yana bir fayl import qilish
              </Button>
            </CardFooter>
          </Card>
        ) : null}
      </div>
    </DashboardShell>
  );
}
