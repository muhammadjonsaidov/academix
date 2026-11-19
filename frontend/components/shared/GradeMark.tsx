import { cn } from "@/lib/utils";

/**
 * The teacher's red-pen grade: a handwritten numeral inside a hand-drawn circle. Only for a
 * grade the teacher confirmed — the AI's suggestion uses <PencilScore> instead (docs/design.md).
 */
export function GradeMark({
  grade,
  size = "md",
  draw = false,
  className,
}: {
  grade: number;
  size?: "sm" | "md" | "lg";
  /** Draw the circle once on mount (respects reduced motion). */
  draw?: boolean;
  className?: string;
}) {
  const box = size === "sm" ? "size-9 text-2xl" : size === "lg" ? "size-20 text-6xl" : "size-12 text-3xl";
  return (
    <span
      role="img"
      aria-label={`Baho: ${grade}`}
      className={cn("relative inline-flex shrink-0 items-center justify-center text-pen-red", box, className)}
    >
      <svg
        viewBox="0 0 48 48"
        aria-hidden="true"
        className={cn("absolute inset-0 size-full -rotate-6", draw && "ink-draw")}
        fill="none"
        stroke="currentColor"
        strokeWidth={2.2}
        strokeLinecap="round"
      >
        {/* Not a perfect circle: it overshoots its start, like a quick pen stroke. */}
        <path pathLength={1} d="M30 6.5C17 4 6 12 6.5 25c.5 12 11 18.5 21 16.5 10-2 15.5-10 14-19.5C40 13 33 7.5 24 8" />
      </svg>
      <span aria-hidden="true" className="font-hand relative leading-none">
        {grade}
      </span>
    </span>
  );
}

/**
 * The AI's suggestion, written in pencil: provisional until the teacher confirms it.
 */
export function PencilScore({
  percent,
  grade,
  className,
}: {
  percent: number;
  grade?: number | null;
  className?: string;
}) {
  return (
    <span
      className={cn(
        "inline-flex items-baseline gap-1 border-b border-dashed border-pencil/60 font-data text-pencil",
        className,
      )}
      title="AI taklifi — yakuniy bahoni o'qituvchi qo'yadi"
    >
      {Math.round(percent)}%
      {grade ? <span className="text-xs">({grade})</span> : null}
    </span>
  );
}
