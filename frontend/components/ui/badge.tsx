import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

/**
 * Badge carries three independent semantic systems on one primitive:
 *  - shadcn base set (default/secondary/outline/destructive) for generic UI chrome
 *  - severity-* (LOW/MEDIUM/HIGH/CRITICAL) for psychologist signals — variant
 *    key is the lowercased severity, e.g. `variant={`severity-${severity.toLowerCase()}`}`.
 *    CRITICAL is deliberately the only *solid*-fill variant in the whole
 *    scale (the rest are tinted-text-on-tint), so it doesn't read as "one
 *    more shade of orange."
 *  - status-* for the AI pipeline (AI_PROCESSING/AI_SKIPPED/AI_DONE) — pair
 *    status-processing with <Spinner />, status-skipped with an icon that
 *    reads "a human took over" (e.g. lucide UserCheck), never an AI/robot icon.
 *  - role-* for the five dashboards' accent chips (avatar tags, "invited as
 *    TEACHER" pills, etc.) — never for primary actions/buttons.
 *  - flagged / ready are semantic aliases over severity-medium / success,
 *    named for the bulk-review "Ko'rib chiqish kerak" vs "Tayyor" grouping
 *    so call sites read as business intent, not raw color choice.
 */
const badgeVariants = cva(
  "inline-flex w-fit shrink-0 items-center gap-1 rounded-md border px-2 py-0.5 text-xs font-medium whitespace-nowrap transition-colors [&>svg]:pointer-events-none [&>svg]:size-3",
  {
    variants: {
      variant: {
        default: "border-transparent bg-primary text-primary-foreground",
        secondary: "border-transparent bg-secondary text-secondary-foreground",
        outline: "border-border bg-transparent text-foreground",
        destructive: "border-transparent bg-destructive/10 text-destructive",

        success: "border-transparent bg-success-bg text-success",
        ready: "border-transparent bg-success-bg text-success",

        "severity-low": "border-transparent bg-severity-low-bg text-severity-low",
        "severity-medium": "border-transparent bg-severity-medium-bg text-severity-medium",
        "severity-high": "border-transparent bg-severity-high-bg text-severity-high",
        "severity-critical": "border-transparent bg-severity-critical text-severity-critical-foreground",
        flagged: "border-severity-medium/30 bg-severity-medium-bg text-severity-medium",

        "status-processing": "border-transparent bg-status-processing-bg text-status-processing",
        "status-done": "border-transparent bg-success-bg text-success",
        "status-skipped": "border-transparent bg-status-skipped-bg text-status-skipped",

        "role-admin": "border-transparent bg-role-admin-muted text-role-admin",
        "role-teacher": "border-transparent bg-role-teacher-muted text-role-teacher",
        "role-student": "border-transparent bg-role-student-muted text-role-student",
        "role-parent": "border-transparent bg-role-parent-muted text-role-parent",
        "role-psychologist": "border-transparent bg-role-psychologist-muted text-role-psychologist",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

function Badge({
  className,
  variant,
  ...props
}: React.ComponentProps<"span"> & VariantProps<typeof badgeVariants>) {
  return (
    <span data-slot="badge" className={cn(badgeVariants({ variant, className }))} {...props} />
  )
}

export { Badge, badgeVariants }
