import { UploadCloud } from "lucide-react";
import { cn } from "@/lib/utils";

interface FileFieldProps
  extends Omit<React.ComponentProps<"input">, "type" | "className" | "children"> {
  id: string;
  /** Chosen file name(s) shown in the row (truncated); empty falls back to `hint`. */
  fileName?: string | null;
  /** Accepted-types hint shown while nothing is chosen, e.g. "PDF, DOCX, JPG yoki PNG". */
  hint?: string;
  /** Text of the button-look segment, defaults to "Fayl tanlang". */
  buttonLabel?: string;
  className?: string;
}

/**
 * Styled file input — replaces every raw `<input type="file">` (whose browser-native
 * "Choose File" chrome ignored the design system entirely). Reads as a dashed
 * drop-zone-style row: an upload-icon button-look segment, then the chosen file name
 * or the accepted-types hint. The native input stays in the DOM (sr-only) and is
 * label-connected, so keyboard focus, form validation (`required`) and screen readers
 * all keep working; the visible row mirrors its focus ring via `has-[]`.
 */
export function FileField({
  id,
  fileName,
  hint,
  buttonLabel = "Fayl tanlang",
  className,
  ...props
}: FileFieldProps) {
  return (
    <label
      htmlFor={id}
      className={cn(
        "flex h-9 w-full cursor-pointer items-stretch overflow-hidden rounded-md border border-dashed border-input bg-background text-sm transition-colors",
        "hover:border-ring/60 hover:bg-muted/40",
        "has-[input:focus-visible]:border-ring has-[input:focus-visible]:ring-3 has-[input:focus-visible]:ring-ring/50",
        "has-[input:disabled]:pointer-events-none has-[input:disabled]:opacity-60",
        className,
      )}
    >
      <span className="flex shrink-0 items-center gap-1.5 border-r border-dashed border-input bg-muted/60 px-3 font-medium text-foreground">
        <UploadCloud className="size-4 text-muted-foreground" strokeWidth={1.75} />
        {buttonLabel}
      </span>
      <span
        className={cn(
          "flex min-w-0 flex-1 items-center px-3",
          fileName ? "text-foreground" : "text-muted-foreground",
        )}
      >
        <span className="truncate">{fileName || hint || "Fayl tanlanmagan"}</span>
      </span>
      <input id={id} type="file" className="sr-only" {...props} />
    </label>
  );
}
