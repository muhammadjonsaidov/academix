"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { SelectField } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const PAGE_SIZES = [10, 20, 50, 100];

interface PaginationControlProps {
  /** 0-based page index (matches the backend's `page` param). */
  page: number;
  size: number;
  totalItems: number;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
  className?: string;
}

/** Shared server-pagination row for paged list endpoints: page-size select on the
 * left, "X–Y / Z" range + prev/next on the right. Renders nothing when the whole
 * list fits the smallest page size and we're on page 0 — no controls for tiny lists. */
export function PaginationControl({
  page,
  size,
  totalItems,
  onPageChange,
  onSizeChange,
  className,
}: PaginationControlProps) {
  if (totalItems <= PAGE_SIZES[0] && page === 0) {
    return null;
  }

  const from = totalItems === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalItems);
  const lastPage = Math.max(Math.ceil(totalItems / size) - 1, 0);

  return (
    <div className={cn("flex flex-wrap items-center justify-between gap-3 text-sm", className)}>
      <div className="flex items-center gap-2 text-muted-foreground">
        <span>Sahifada:</span>
        <SelectField
          aria-label="Sahifadagi elementlar soni"
          className="w-20"
          value={String(size)}
          onChange={(e) => onSizeChange(Number(e.target.value))}
        >
          {PAGE_SIZES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </SelectField>
      </div>
      <div className="flex items-center gap-2">
        <span className="font-data text-muted-foreground">
          {from}–{to} / {totalItems}
        </span>
        <Button
          type="button"
          variant="outline"
          size="icon-sm"
          aria-label="Oldingi sahifa"
          disabled={page === 0}
          onClick={() => onPageChange(page - 1)}
        >
          <ChevronLeft className="size-4" strokeWidth={1.75} />
        </Button>
        <Button
          type="button"
          variant="outline"
          size="icon-sm"
          aria-label="Keyingi sahifa"
          disabled={page >= lastPage}
          onClick={() => onPageChange(page + 1)}
        >
          <ChevronRight className="size-4" strokeWidth={1.75} />
        </Button>
      </div>
    </div>
  );
}
