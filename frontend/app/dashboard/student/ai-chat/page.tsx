"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { StudentNav } from "@/components/student/StudentNav";
import { Button } from "@/components/ui/button";
import { useAiChatStore } from "@/stores/useAiChatStore";
import type { SubjectType } from "@/types/aiChat";

const SUBJECTS: { value: SubjectType | "GENERAL"; label: string }[] = [
  { value: "GENERAL", label: "Umumiy" },
  { value: "MATH", label: "Matematika" },
  { value: "LANGUAGE_UZ", label: "O'zbek tili" },
  { value: "LANGUAGE_RU", label: "Rus tili" },
  { value: "LANGUAGE_EN", label: "Ingliz tili" },
  { value: "PHYSICS", label: "Fizika" },
  { value: "CHEMISTRY", label: "Kimyo" },
  { value: "BIOLOGY", label: "Biologiya" },
  { value: "HISTORY", label: "Tarix" },
  { value: "GEOGRAPHY", label: "Geografiya" },
];

export default function AiChatPage() {
  const history = useAiChatStore((state) => state.history);
  const isSending = useAiChatStore((state) => state.isSending);
  const fetchHistory = useAiChatStore((state) => state.fetchHistory);
  const sendMessage = useAiChatStore((state) => state.sendMessage);

  const [subject, setSubject] = useState<SubjectType | "GENERAL">("GENERAL");
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchHistory().catch(() => setError("Suhbat tarixini yuklab bo'lmadi."));
  }, [fetchHistory]);

  async function handleSend() {
    setError(null);
    if (!message.trim()) return;
    try {
      await sendMessage({ subject, message: message.trim() });
      setMessage("");
    } catch {
      setError("Xabar yuborib bo'lmadi.");
    }
  }

  // Backend returns newest-first (inbox convention) — reversed here for chronological chat display.
  const chronological = [...history].reverse();

  return (
    <DashboardShell role="STUDENT">
      <StudentNav />
      <h2 className="mb-4 text-lg font-semibold">AI Tutor</h2>

      <div className="mb-4">
        <label className="mb-1 block text-xs text-muted-foreground">Fan</label>
        <select
          className="rounded-md border border-border bg-background px-2 py-1.5 text-sm"
          value={subject}
          onChange={(e) => setSubject(e.target.value as SubjectType | "GENERAL")}
        >
          {SUBJECTS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </select>
      </div>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}

      <div className="mb-4 flex max-h-[50vh] flex-col gap-3 overflow-y-auto rounded-md border border-border p-4">
        {chronological.length === 0 ? (
          <p className="text-sm text-muted-foreground">Suhbat hali yo&apos;q. Savolingizni yozing.</p>
        ) : (
          chronological.map((item) => (
            <div key={item.id} className="space-y-1">
              <div className="ml-auto max-w-[80%] rounded-lg bg-primary px-3 py-2 text-sm text-primary-foreground">
                {item.message}
              </div>
              <div
                className={`mr-auto max-w-[80%] rounded-lg px-3 py-2 text-sm ${
                  item.isBlocked ? "bg-muted text-muted-foreground" : "bg-secondary"
                }`}
              >
                {item.response}
              </div>
            </div>
          ))
        )}
      </div>

      <div className="flex gap-2">
        <input
          className="flex-1 rounded-md border border-border bg-background px-3 py-2 text-sm"
          placeholder="Savolingizni yozing..."
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleSend();
          }}
        />
        <Button onClick={handleSend} disabled={isSending}>
          {isSending ? "Yuborilmoqda..." : "Yuborish"}
        </Button>
      </div>
    </DashboardShell>
  );
}
