"use client";

import { useEffect, useState } from "react";
import { MessageSquareText, Send, ShieldAlert } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { EmptyState } from "@/components/student/EmptyState";
import { useAiChatStore } from "@/stores/useAiChatStore";
import type { SubjectType } from "@/types/aiChat";
import { cn } from "@/lib/utils";

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
      <h2 className="mb-4 font-heading text-lg font-semibold">AI Tutor</h2>

      <div className="mb-4">
        <label className="mb-1 block text-xs font-medium text-muted-foreground">Fan</label>
        <select
          className="rounded-md border border-border bg-background px-2.5 py-1.5 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
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

      <Card className="mb-4">
        <CardContent className="flex max-h-[50vh] flex-col gap-3 overflow-y-auto">
          {chronological.length === 0 ? (
            <EmptyState
              icon={MessageSquareText}
              title="Suhbat hali yo'q"
              description="AI Tutor'dan fan bo'yicha savolingizni so'rang."
            />
          ) : (
            chronological.map((item) => (
              <div key={item.id} className="space-y-1">
                <div className="ml-auto max-w-[80%] rounded-lg rounded-br-sm bg-role-student px-3 py-2 text-sm text-role-student-foreground">
                  {item.message}
                </div>
                <div
                  className={cn(
                    "mr-auto flex max-w-[80%] items-start gap-1.5 rounded-lg rounded-bl-sm px-3 py-2 text-sm",
                    item.isBlocked
                      ? "bg-severity-medium-bg text-severity-medium"
                      : "bg-secondary text-secondary-foreground",
                  )}
                >
                  {item.isBlocked ? (
                    <ShieldAlert className="mt-0.5 size-3.5 shrink-0" strokeWidth={1.75} />
                  ) : null}
                  <span>{item.response}</span>
                </div>
              </div>
            ))
          )}
        </CardContent>
      </Card>

      <div className="flex gap-2">
        <input
          className="flex-1 rounded-md border border-border bg-background px-3 py-2 text-sm outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          placeholder="Savolingizni yozing..."
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleSend();
          }}
        />
        <Button onClick={handleSend} disabled={isSending}>
          <Send className="size-4" strokeWidth={1.75} />
          {isSending ? "Yuborilmoqda..." : "Yuborish"}
        </Button>
      </div>
    </DashboardShell>
  );
}
