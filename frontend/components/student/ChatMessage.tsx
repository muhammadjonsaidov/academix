import Markdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";
import { Bot, ShieldAlert } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * AI Tutor answers are markdown: headings, lists, bold, the odd table, `$…$` for math. They render
 * as chat text, not a document: headings stay small, lists keep their markers, math reads as
 * notation (`$…$` becomes inline code, styled with the data font). Raw HTML and images are not
 * rendered.
 */
const MATH = /\$([^$\n]+)\$/g;

const TUTOR_COMPONENTS: Components = {
  h1: ({ children }) => <p className="mt-3 mb-1 text-[0.9375rem] font-semibold first:mt-0">{children}</p>,
  h2: ({ children }) => <p className="mt-3 mb-1 font-semibold first:mt-0">{children}</p>,
  h3: ({ children }) => <p className="mt-2 mb-1 font-semibold first:mt-0">{children}</p>,
  h4: ({ children }) => <p className="mt-2 mb-1 font-medium first:mt-0">{children}</p>,
  p: ({ children }) => <p className="my-2 first:mt-0 last:mb-0">{children}</p>,
  ul: ({ children }) => <ul className="my-2 list-disc space-y-1 pl-5 first:mt-0 last:mb-0">{children}</ul>,
  ol: ({ children }) => <ol className="my-2 list-decimal space-y-1 pl-5 first:mt-0 last:mb-0">{children}</ol>,
  li: ({ children }) => <li className="pl-0.5 marker:text-muted-foreground">{children}</li>,
  strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
  code: ({ children }) => <code className="font-data text-[0.92em]">{children}</code>,
  pre: ({ children }) => (
    <pre className="my-2 overflow-x-auto rounded-md bg-muted px-3 py-2 whitespace-pre">{children}</pre>
  ),
  blockquote: ({ children }) => (
    <blockquote className="my-2 border-l-2 border-katak pl-3 text-muted-foreground">{children}</blockquote>
  ),
  hr: () => <hr className="my-3 border-border" />,
  a: ({ href, children }) => (
    <a href={href} target="_blank" rel="noreferrer" className="underline underline-offset-2">
      {children}
    </a>
  ),
  table: ({ children }) => (
    <div className="my-2 overflow-x-auto">
      <table className="border-collapse text-left">{children}</table>
    </div>
  ),
  th: ({ children }) => <th className="border border-border px-2 py-1 font-semibold">{children}</th>,
  td: ({ children }) => <td className="border border-border px-2 py-1">{children}</td>,
  img: () => null,
};

export function TutorText({ text }: { text: string }) {
  return (
    <Markdown remarkPlugins={[remarkGfm]} components={TUTOR_COMPONENTS}>
      {text.replace(MATH, "`$1`")}
    </Markdown>
  );
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
          "min-w-0 rounded-2xl rounded-bl-md px-4 py-2.5 text-sm leading-relaxed",
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
        <TutorText text={text} />
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
