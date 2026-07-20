import { cn } from "@/lib/utils";

/** Shared input/select classes for admin forms — no <Input>/<Select> primitive exists in
 * components/ui yet, so this keeps every admin form field visually consistent (matches
 * Button's focus-ring treatment) without inventing a one-off style per page. */
export const fieldClass =
  "h-9 rounded-md border border-input bg-background px-3 text-sm outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50";

interface FormFieldProps {
  label: string;
  htmlFor: string;
  className?: string;
  children: React.ReactNode;
}

export function FormField({ label, htmlFor, className, children }: FormFieldProps) {
  return (
    <div className={cn("space-y-1.5", className)}>
      <label htmlFor={htmlFor} className="text-sm font-medium text-foreground">
        {label}
      </label>
      {children}
    </div>
  );
}
