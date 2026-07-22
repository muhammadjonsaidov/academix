import type { CSSProperties } from "react";

/**
 * Shared Recharts styling for all analytics pages — every color is a CSS token
 * (never a hex literal), so charts flip automatically with the `.dark` class.
 * Series colors themselves come from `var(--color-chart-1)`..`var(--color-chart-5)` /
 * `var(--graphite)` / `var(--success)` at each call site.
 */

/** Category axis ticks — small, muted, sans. */
export const chartAxisTick = { fontSize: 11, fill: "var(--muted-foreground)" };

/** Numeric axis ticks — same, but in the mono `font-data` face (tabular numbers). */
export const chartDataTick = {
  fontSize: 11,
  fill: "var(--muted-foreground)",
  fontFamily: "var(--font-mono)",
};

/** Subtle grid, always the border token. */
export const chartGridProps = {
  stroke: "var(--border)",
  strokeDasharray: "3 3",
} as const;

/** Tooltip panel styled as a small card (bg-card/border tokens, not Recharts' default white). */
export const chartTooltipStyle: CSSProperties = {
  backgroundColor: "var(--card)",
  border: "1px solid var(--border)",
  borderRadius: "calc(var(--radius) * 0.8)",
  color: "var(--card-foreground)",
  fontSize: 12,
  boxShadow: "0 4px 12px oklch(0 0 0 / 0.08)",
};

export const chartTooltipLabelStyle: CSSProperties = {
  color: "var(--card-foreground)",
  fontWeight: 500,
};

/** Hover cursor for bar charts — border-token wash instead of the default gray block. */
export const chartBarCursor = { fill: "var(--border)", opacity: 0.35 };

/** Hover cursor for line/area charts — a thin border-token rule. */
export const chartLineCursor = { stroke: "var(--border)", strokeWidth: 1 };

export const chartLegendStyle: CSSProperties = { fontSize: 11 };
