import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";

/** Shared input/textarea classes for forms across every role — no <Input>/<Select>
 * primitive exists in components/ui yet, so this keeps every form field visually
 * consistent (matches Button's focus-ring treatment) without inventing a one-off style
 * per page. Deliberately no `w-full` here: call sites own their width (grid cells add
 * w-full, inline table micro-forms keep fixed widths like w-20). */
export const fieldClass =
  "h-9 rounded-md border border-input bg-background px-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-60 focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

interface FormFieldProps {
  label: string;
  htmlFor: string;
  /** Optional helper line under the control — muted, text-xs. */
  hint?: string;
  className?: string;
  children: React.ReactNode;
}

/** Label-above-control field wrapper (6px gap) — the one form-field layout used
 * everywhere, so a form reads as a calm column of labeled controls instead of a
 * cramped inline "Label[control]Label[control]" row. */
export function FormField({ label, htmlFor, hint, className, children }: FormFieldProps) {
  return (
    <div className={cn("flex min-w-0 flex-col gap-1.5", className)}>
      <label htmlFor={htmlFor} className="text-sm font-medium text-foreground">
        {label}
      </label>
      {children}
      {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
    </div>
  );
}

/** Native <select> styled to match fieldClass: appearance-none + a lucide chevron
 * positioned over the right edge (an inline-SVG background can't use theme tokens,
 * an absolutely-positioned icon can). `className` sizes the wrapper — the select
 * itself always fills it. */
export function SelectField({
  className,
  children,
  ...props
}: React.ComponentProps<"select"> & { className?: string }) {
  return (
    <div className={cn("relative", className)}>
      <select {...props} className={cn(fieldClass, "w-full appearance-none pr-8")}>
        {children}
      </select>
      <ChevronDown
        aria-hidden
        className="pointer-events-none absolute top-1/2 right-2.5 size-4 -translate-y-1/2 text-muted-foreground"
        strokeWidth={1.75}
      />
    </div>
  );
}
