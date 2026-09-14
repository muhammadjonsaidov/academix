"use client";

import { useEffect } from "react";
import { AlertCircle, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

/** Local error boundary preserves dashboard chrome and offers a safe retry. */
export default function DashboardError({
  error,
  reset,
}: {
  error: Error;
  reset: () => void;
}) {
  useEffect(() => {
    console.error("Dashboard route error", error);
  }, [error]);
  return (
    <div className="flex min-h-[50vh] items-center justify-center">
      <div className="max-w-sm text-center">
        <AlertCircle className="mx-auto size-8 text-destructive" />
        <h2 className="mt-3 font-heading text-lg font-semibold">
          Ma&apos;lumot yuklanmadi
        </h2>
        <p className="mt-1 text-sm text-muted-foreground">
          Tarmoqni tekshirib, qayta urinib ko&apos;ring.
        </p>
        <Button className="mt-4" onClick={reset}>
          <RefreshCw className="size-4" /> Qayta urinish
        </Button>
      </div>
    </div>
  );
}
