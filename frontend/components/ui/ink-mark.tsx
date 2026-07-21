import { cn } from "@/lib/utils"

interface InkMarkProps extends React.ComponentProps<"svg"> {
  variant: "check" | "cross"
  /** Accessible label. Omit for a purely decorative mark (e.g. sitting next
   *  to text that already says "to'g'ri"/"xato") — it's then aria-hidden. */
  label?: string
}

/**
 * The signature "teacher's red pen" mark — a single, restrained SVG stroke,
 * NOT a handwriting font and NOT a decorative flourish. Sized to sit inline
 * next to text (defaults to 1em, i.e. it scales with surrounding type).
 * Replaces the old rail-verified/rail-skipped colored-border-only treatment
 * on AI-graded work: the mark itself is the signature now, the rail utility
 * in globals.css stays for the card-level accent, this is for inline use
 * next to a score/step/answer.
 *
 * Color is wired to the exact semantic anchor by variant (--chalk-green for
 * check, --pen-red for cross) via the text-chalk-green/text-pen-red
 * utilities added alongside the shared --success/--destructive tokens —
 * override via className only if a call site has a specific reason to.
 */
function InkMark({ variant, label, className, ...props }: InkMarkProps) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      role={label ? "img" : undefined}
      aria-hidden={label ? undefined : true}
      aria-label={label}
      className={cn(
        "inline-block size-[1.1em] shrink-0 align-[-0.15em]",
        variant === "check" ? "text-chalk-green" : "text-pen-red",
        className
      )}
      {...props}
    >
      {variant === "check" ? (
        <path
          d="M4 13c1.8 2.2 3.6 4.4 5.7 6.3C13.6 14 17.1 8.6 20.5 4.2"
          stroke="currentColor"
          strokeWidth={2.5}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      ) : (
        <>
          <path
            d="M5 5.3c4.6 4.7 9.2 9.4 14.2 13.5"
            stroke="currentColor"
            strokeWidth={2.5}
            strokeLinecap="round"
          />
          <path
            d="M19.2 5.6c-4.8 4.5-9.6 9-14.5 12.9"
            stroke="currentColor"
            strokeWidth={2.5}
            strokeLinecap="round"
          />
        </>
      )}
    </svg>
  )
}

export { InkMark }
