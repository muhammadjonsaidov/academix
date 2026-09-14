import { Skeleton } from "@/components/ui/skeleton";

/** Keeps dashboard navigation responsive while route data/code is loading. */
export default function DashboardLoading() {
  return (
    <div className="space-y-6" aria-label="Sahifa yuklanmoqda" aria-busy="true">
      <div className="space-y-2">
        <Skeleton className="h-7 w-56" />
        <Skeleton className="h-4 w-80 max-w-full" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {Array.from({ length: 4 }, (_, index) => (
          <Skeleton key={index} className="h-28 rounded-xl" />
        ))}
      </div>
      <Skeleton className="h-72 rounded-xl" />
    </div>
  );
}
