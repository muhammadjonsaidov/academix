"use client";

import { useEffect, useRef, useState } from "react";
import { MessageSquareText, Send } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { fieldClass, SelectField } from "@/components/shared/FormField";
import { TutorBubble, TypingBubble, UserBubble } from "@/components/student/ChatMessage";
import { Button } from "@/components/ui/button";
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

const SUGGESTIONS = [
  "Kvadrat tenglamani yechish qadamlarini tushuntirib ber",
  "Nyutonning ikkinchi qonunini misol bilan tushuntir",
  "Foizlarni qanday hisoblashni o'rgat",
];

export default function AiChatPage() {
  const history = useAiChatStore((state) => state.history);
  const pendingMessage = useAiChatStore((state) => state.pendingMessage);
  const isSending = useAiChatStore((state) => state.isSending);
  const fetchHistory = useAiChatStore((state) => state.fetchHistory);
  const sendMessage = useAiChatStore((state) => state.sendMessage);

  const [subject, setSubject] = useState<SubjectType | "GENERAL">("GENERAL");
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    fetchHistory().catch(() => setError("Suhbat tarixini yuklab bo'lmadi."));
  }, [fetchHistory]);

  // Keep the newest message in view — DOM scrolling is an external-system effect, allowed.
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ block: "end" });
  }, [history, pendingMessage]);

  async function handleSend(text?: string) {
    const body = (text ?? message).trim();
    if (!body || isSending) return;
    setError(null);
    setMessage("");
    try {
      await sendMessage({ subject, message: body });
    } catch {
      setError("Xabar yuborib bo'lmadi. Qayta urinib ko'ring.");
      setMessage(body);
    }
  }

  // Backend returns newest-first (inbox convention) — reversed for chronological display.
  const chronological = [...history].reverse();
  const isEmpty = chronological.length === 0 && !pendingMessage;

  return (
    <DashboardShell role="STUDENT">
      {/* Full-height chat: messages scroll, composer stays pinned at the bottom. */}
      <div className="mx-auto flex h-[calc(100vh-7.5rem)] max-w-3xl flex-col">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
          <h2 className="font-heading text-lg font-semibold">AI Tutor</h2>
          <SelectField
            id="chat-subject"
            aria-label="Fan"
            className="w-44"
            value={subject}
            onChange={(e) => setSubject(e.target.value as SubjectType | "GENERAL")}
          >
            {SUBJECTS.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </SelectField>
        </div>

        <div className="flex-1 space-y-4 overflow-y-auto rounded-lg border border-border bg-background/60 p-4">
          {isEmpty ? (
            <div className="flex h-full flex-col items-center justify-center gap-5 text-center">
              <span className="flex size-12 items-center justify-center rounded-2xl bg-accent-role-muted text-accent-role">
                <MessageSquareText className="size-6" strokeWidth={1.75} />
              </span>
              <div>
                <p className="font-heading text-base font-semibold">
                  AI Tutor bilan suhbatni boshlang
                </p>
                <p className="mt-1 text-sm text-muted-foreground">
                  Javobni aytib bermaydi — yechishga o&apos;zingizni yo&apos;naltiradi.
                </p>
              </div>
              <div className="flex flex-col items-stretch gap-2">
                {SUGGESTIONS.map((s) => (
                  <button
                    key={s}
                    type="button"
                    onClick={() => handleSend(s)}
                    className="card-lift rounded-lg border border-border bg-card px-4 py-2.5 text-left text-sm text-muted-foreground hover:text-foreground"
                  >
                    {s}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <>
              {chronological.map((item) => (
                <div key={item.id} className="space-y-3">
                  <UserBubble text={item.message} />
                  <TutorBubble text={item.response} isBlocked={item.isBlocked} />
                </div>
              ))}
              {pendingMessage ? (
                <div className="space-y-3">
                  <UserBubble text={pendingMessage} />
                  <TypingBubble />
                </div>
              ) : null}
            </>
          )}
          <div ref={bottomRef} />
        </div>

        {error ? (
          <p role="alert" className="mt-2 text-sm text-destructive">
            {error}
          </p>
        ) : null}

        <div className="mt-3 flex items-end gap-2">
          <textarea
            className={cn(fieldClass, "max-h-32 min-h-11 w-full flex-1 resize-none py-2.5")}
            rows={1}
            aria-label="Savolingiz"
            placeholder="Savolingizni yozing... (Enter — yuborish)"
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
          />
          <Button
            size="icon-lg"
            aria-label="Yuborish"
            onClick={() => handleSend()}
            disabled={isSending || !message.trim()}
          >
            <Send className="size-4" strokeWidth={1.75} />
          </Button>
        </div>
        <p className="mt-1.5 text-xs text-muted-foreground">
          AI Tutor tayyor javoblarni bermaydi — qadam-baqadam o&apos;rganishga yordam beradi.
        </p>
      </div>
    </DashboardShell>
  );
}
