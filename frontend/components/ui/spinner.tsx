import { Loader2 } from "lucide-react"

import { cn } from "@/lib/utils"

interface SpinnerProps extends React.ComponentProps<"span"> {
  /** Screen-reader label; visually hidden. Defaults to a neutral Uzbek label. */
  label?: string
}

/**
 * Inline AI-processing indicator — pair with
 * <Badge variant="status-processing"> for the AI_PROCESSING state so it
 * reads distinctly from both the Skeleton (loading) and a plain muted-gray
 * paragraph (the old, ambiguous treatment).
 */
function Spinner({ className, label = "Yuklanmoqda", ...props }: SpinnerProps) {
  return (
    <span
      data-slot="spinner"
      role="status"
      className={cn("inline-flex items-center", className)}
      {...props}
    >
      <Loader2
        aria-hidden="true"
        className="size-3.5 animate-spin text-status-processing"
        strokeWidth={1.75}
      />
      <span className="sr-only">{label}</span>
    </span>
  )
}

export { Spinner }
