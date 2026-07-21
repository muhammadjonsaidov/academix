import { cn } from "@/lib/utils"

const STAMP_VARIANTS = {
  ink: "border-ink text-ink",
  "chalk-green": "border-chalk-green text-chalk-green",
} as const

const STAMP_SIZES = {
  sm: "size-9 text-xs",
  md: "size-12 text-sm",
  lg: "size-16 text-base",
} as const

interface StampBadgeProps extends React.ComponentProps<"span"> {
  variant?: keyof typeof STAMP_VARIANTS
  size?: keyof typeof STAMP_SIZES
}

/**
 * Circular "ink stamp" treatment for achievement/XP badges — wraps a short
 * icon (emoji/lucide) or 1-2 characters of text. The barely-asymmetric
 * border-radius plus a slight tilt reads as hand-stamped without tipping
 * into novelty/kitsch — restraint matters more than the effect here, per
 * the design brief. Deterministic (no per-instance randomness), so it's
 * safe in a server component and identical across a grid of badges.
 *
 * Replaces the generic badge-icon circle (bg-role-student-muted) previously
 * used on the student badges page.
 */
function StampBadge({
  variant = "ink",
  size = "md",
  className,
  children,
  ...props
}: StampBadgeProps) {
  return (
    <span
      data-slot="stamp-badge"
      className={cn(
        "inline-flex shrink-0 -rotate-2 items-center justify-center border-2 bg-card font-heading font-semibold",
        "rounded-[50%_48%_52%_49%/48%_52%_49%_51%]",
        STAMP_SIZES[size],
        STAMP_VARIANTS[variant],
        className
      )}
      {...props}
    >
      {children}
    </span>
  )
}

export { StampBadge }
