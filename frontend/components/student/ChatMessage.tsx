import { Bot, ShieldAlert } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Lightweight rich-text rendering for AI Tutor answers — Qwen replies use markdown-ish
 * `**bold**`, `$inline math$` and newlines. A full markdown/KaTeX stack is overkill for a
 * tutoring chat; this covers what the model actually emits: bold → <strong>, $…$ → a
 * font-data span (reads as notation), newlines preserved by whitespace-pre-wrap.
 */
function renderRichText(text: string): React.ReactNode[] {
  const parts = text.split(/(\*\*[^*]+\*\*|\$[^$\n]+\$)/g);
  return parts.map((part, i) => {
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={i}>{part.slice(2, -2)}</strong>;
    }
    if (part.startsWith("$") && part.endsWith("$") && part.length > 2) {
      return (
        <span key={i} className="font-data text-[0.92em]">
          {part.slice(1, -1)}
        </span>
      );
    }
    return part;
  });
}

export function UserBubble({ text }: { text: string }) {
  return (
    <div className="animate-rise ml-auto max-w-[85%] rounded-2xl rounded-br-md bg-accent-role-muted px-4 py-2.5 text-sm whitespace-pre-wrap sm:max-w-[70%]">
      {text}
    </div>
  );
}

export function TutorBubble({
  text,
  isBlocked,
}: {
  text: string;
  isBlocked?: boolean;
}) {
  return (
    <div className="animate-rise mr-auto flex max-w-[92%] items-start gap-2.5 sm:max-w-[80%]">
      <span className="mt-1 flex size-7 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground">
        <Bot className="size-4" strokeWidth={1.75} />
      </span>
      <div
        className={cn(
          "rounded-2xl rounded-bl-md px-4 py-2.5 text-sm leading-relaxed whitespace-pre-wrap",
          isBlocked
            ? "bg-severity-medium-bg text-severity-medium"
            : "bg-card border border-border",
        )}
      >
        {isBlocked ? (
          <span className="mb-1 flex items-center gap-1.5 font-medium">
            <ShieldAlert className="size-3.5 shrink-0" strokeWidth={1.75} />
            Javob cheklandi
          </span>
        ) : null}
        {renderRichText(text)}
      </div>
    </div>
  );
}

/** Three-dot typing indicator shown while Qwen is thinking. */
export function TypingBubble() {
  return (
    <div className="animate-rise mr-auto flex items-start gap-2.5">
      <span className="mt-1 flex size-7 shrink-0 items-center justify-center rounded-full bg-primary text-primary-foreground">
        <Bot className="size-4" strokeWidth={1.75} />
      </span>
      <div className="flex items-center gap-1 rounded-2xl rounded-bl-md border border-border bg-card px-4 py-3">
        <span className="typing-dot" />
        <span className="typing-dot" style={{ animationDelay: "0.15s" }} />
        <span className="typing-dot" style={{ animationDelay: "0.3s" }} />
      </div>
    </div>
  );
}
