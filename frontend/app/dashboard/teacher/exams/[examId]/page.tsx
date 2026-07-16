"use client";

import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { useParams } from "next/navigation";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { TeacherNav } from "@/components/teacher/TeacherNav";
import { Button } from "@/components/ui/button";
import { useExamStore } from "@/stores/useExamStore";
import { useTeacherStore } from "@/stores/useTeacherStore";
import type { ApiErrorResponse } from "@/types/auth";
import type { ExamSubmission } from "@/types/teacher";

function ExamSubmissionRow({ examId, submission }: { examId: string; submission: ExamSubmission }) {
  const gradeExamSubmission = useExamStore((state) => state.gradeExamSubmission);
  const scoreRef = useRef<HTMLInputElement>(null);
  const [fivePointGrade, setFivePointGrade] = useState("5");
  const [teacherComment, setTeacherComment] = useState("");
  const [isGrading, setIsGrading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const defaultScore = submission.aiFeedback
    ? String(Math.round(submission.aiFeedback.aiScorePercent))
    : "";

  async function handleGrade(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setIsGrading(true);
    try {
      await gradeExamSubmission(examId, submission.submissionId, {
        score: Number(scoreRef.current?.value ?? 0),
        fivePointGrade: Number(fivePointGrade),
        teacherComment,
      });
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Baholab bo'lmadi.");
    } finally {
      setIsGrading(false);
    }
  }

  return (
    <tr className={submission.flaggedForReview ? "bg-amber-50" : ""}>
      <td className="py-2 align-top font-medium">{submission.studentName}</td>
      <td className="py-2 align-top">
        {submission.status}
        {submission.flaggedForReview ? (
          <div className="text-xs text-amber-600">⚠ Ko&apos;rib chiqish kerak</div>
        ) : null}
      </td>
      <td className="py-2 align-top">{submission.aiFeedback?.aiScorePercent ?? "—"}</td>
      <td className="py-2 align-top">
        {submission.status === "GRADED" ? (
          <span>
            {submission.score} ({submission.fivePointGrade})
          </span>
        ) : (
          <form onSubmit={handleGrade} className="flex flex-wrap items-end gap-2">
            <input
              key={submission.submissionId}
              ref={scoreRef}
              type="number"
              min={0}
              max={100}
              defaultValue={defaultScore}
              required
              placeholder="Ball"
              className="w-20 rounded-md border border-input bg-background px-2 py-1 text-sm"
            />
            <input
              type="number"
              min={2}
              max={5}
              value={fivePointGrade}
              onChange={(e) => setFivePointGrade(e.target.value)}
              required
              className="w-16 rounded-md border border-input bg-background px-2 py-1 text-sm"
            />
            <input
              value={teacherComment}
              onChange={(e) => setTeacherComment(e.target.value)}
              placeholder="Izoh"
              className="rounded-md border border-input bg-background px-2 py-1 text-sm"
            />
            <Button type="submit" size="sm" disabled={isGrading}>
              {isGrading ? "..." : "Baholash"}
            </Button>
          </form>
        )}
        {error ? <p className="text-xs text-destructive">{error}</p> : null}
      </td>
    </tr>
  );
}

export default function TeacherExamDetailPage() {
  const params = useParams<{ examId: string }>();
  const examId = params.examId;

  const exams = useExamStore((state) => state.exams);
  const fetchExams = useExamStore((state) => state.fetchExams);
  const examSubmissions = useExamStore((state) => state.examSubmissions);
  const fetchExamSubmissions = useExamStore((state) => state.fetchExamSubmissions);
  const bulkUploadSubmissions = useExamStore((state) => state.bulkUploadSubmissions);
  const approveAllExamSubmissions = useExamStore((state) => state.approveAllExamSubmissions);
  const classStudents = useTeacherStore((state) => state.classStudents);
  const fetchClassStudents = useTeacherStore((state) => state.fetchClassStudents);

  const [files, setFiles] = useState<File[]>([]);
  const [studentIds, setStudentIds] = useState<string[]>([]);
  const [uploadMessage, setUploadMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [isApprovingAll, setIsApprovingAll] = useState(false);

  const exam = useMemo(() => exams.find((e) => e.examId === examId), [exams, examId]);

  useEffect(() => {
    fetchExams().catch(() => {});
    fetchExamSubmissions(examId).catch(() => setError("Topshiriqlarni yuklab bo'lmadi."));
  }, [fetchExams, fetchExamSubmissions, examId]);

  useEffect(() => {
    if (exam) {
      fetchClassStudents(exam.classId).catch(() => {});
    }
  }, [exam, fetchClassStudents]);

  function handleFilesChange(selected: File[]) {
    setFiles(selected);
    setStudentIds(selected.map(() => ""));
  }

  function handleStudentIdChange(index: number, studentId: string) {
    setStudentIds((prev) => prev.map((id, i) => (i === index ? studentId : id)));
  }

  async function handleUpload() {
    setError(null);
    setUploadMessage(null);
    if (files.length === 0 || studentIds.some((id) => !id)) {
      setError("Har bir rasm uchun o'quvchini tanlang.");
      return;
    }
    setIsUploading(true);
    try {
      const { queued } = await bulkUploadSubmissions(examId, files, studentIds);
      setUploadMessage(`${queued} ta qog'oz navbatga qo'yildi. AI tahlil qilmoqda...`);
      setFiles([]);
      setStudentIds([]);
      await fetchExamSubmissions(examId);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Yuklab bo'lmadi.");
    } finally {
      setIsUploading(false);
    }
  }

  async function handleApproveAll() {
    setError(null);
    setIsApprovingAll(true);
    try {
      await approveAllExamSubmissions(examId);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Tasdiqlab bo'lmadi.");
    } finally {
      setIsApprovingAll(false);
    }
  }

  const flaggedCount = examSubmissions.filter((s) => s.flaggedForReview).length;

  return (
    <DashboardShell role="TEACHER">
      <TeacherNav />
      <h2 className="mb-1 text-lg font-semibold">{exam?.title ?? "Imtihon"}</h2>
      <p className="mb-6 text-sm text-muted-foreground">
        {exam ? `${exam.examDate} · ${exam.submissionsCount} ta topshirildi` : ""}
      </p>

      <div className="mb-6 space-y-3 rounded-md border border-border p-4">
        <p className="font-medium">Qog&apos;ozlarni ommaviy yuklash</p>
        <input
          type="file"
          multiple
          accept="image/*"
          onChange={(e) => handleFilesChange(Array.from(e.target.files ?? []))}
        />
        {files.length > 0 ? (
          <ul className="space-y-2 text-sm">
            {files.map((file, index) => (
              <li key={`${file.name}-${index}`} className="flex items-center gap-2">
                <span className="w-48 truncate">{file.name}</span>
                <select
                  value={studentIds[index] ?? ""}
                  onChange={(e) => handleStudentIdChange(index, e.target.value)}
                  className="rounded-md border border-input bg-background px-2 py-1 text-sm"
                >
                  <option value="" disabled>
                    O&apos;quvchini tanlang
                  </option>
                  {classStudents.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.firstName} {s.lastName}
                    </option>
                  ))}
                </select>
              </li>
            ))}
          </ul>
        ) : null}
        <Button type="button" onClick={handleUpload} disabled={isUploading || files.length === 0}>
          {isUploading ? "Yuklanmoqda..." : `Yuklash (${files.length} ta rasm)`}
        </Button>
        {uploadMessage ? <p className="text-sm text-green-600">{uploadMessage}</p> : null}
      </div>

      {flaggedCount > 0 ? (
        <div className="mb-4 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-800">
          {flaggedCount} ta ish qo&apos;lda ko&apos;rib chiqilishi kerak (yozuv moslik past yoki
          skan sifati past).
        </div>
      ) : null}

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <table className="mb-4 w-full text-left text-sm">
        <thead className="text-muted-foreground">
          <tr className="border-b border-border">
            <th className="py-2">O&apos;quvchi</th>
            <th className="py-2">Holat</th>
            <th className="py-2">AI ball</th>
            <th className="py-2">Baho</th>
          </tr>
        </thead>
        <tbody>
          {examSubmissions.map((submission) => (
            <ExamSubmissionRow key={submission.submissionId} examId={examId} submission={submission} />
          ))}
          {examSubmissions.length === 0 ? (
            <tr>
              <td colSpan={4} className="py-4 text-center text-muted-foreground">
                Hozircha topshiriqlar yo&apos;q.
              </td>
            </tr>
          ) : null}
        </tbody>
      </table>

      <Button
        type="button"
        variant="secondary"
        onClick={handleApproveAll}
        disabled={isApprovingAll || flaggedCount === examSubmissions.length}
      >
        {isApprovingAll ? "Tasdiqlanmoqda..." : "Belgilanmaganlarni ommaviy tasdiqlash"}
      </Button>
    </DashboardShell>
  );
}
