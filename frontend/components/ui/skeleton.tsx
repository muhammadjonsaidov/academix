import { cn } from "@/lib/utils"

/**
 * Loading-state primitive. Distinct on purpose from AI_PROCESSING (which
 * uses <Spinner />/<Badge variant="status-processing" />): a Skeleton means
 * "we don't have the shape of the data yet" (network request in flight), a
 * processing badge means "we have the row, the AI just hasn't finished
 * grading it yet." Don't reach for this to represent AI_PROCESSING.
 */
function Skeleton({ className, ...props }: React.ComponentProps<"div">) {
  return (
    <div
      data-slot="skeleton"
      className={cn("bg-muted animate-pulse rounded-md", className)}
      {...props}
    />
  )
}

export { Skeleton }
