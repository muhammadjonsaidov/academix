"use client";

import { useEffect, useState } from "react";
import { ShieldOff } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { EmptyState } from "@/components/shared/EmptyState";
import { PageHeader } from "@/components/shared/PageHeader";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";
import type { ApiErrorResponse } from "@/types/auth";

const STATUS_LABEL: Record<string, string> = {
  PENDING: "Kutilmoqda",
  APPROVED: "Tasdiqlangan",
  REJECTED: "Rad etilgan",
};

export default function AdminDataDeletionRequestsPage() {
  const requests = useAdminStore((state) => state.dataDeletionRequests);
  // Full unpaginated list — name lookups must cover every student, not one page.
  const students = useAdminStore((state) => state.allStudents);
  const fetchDataDeletionRequests = useAdminStore((state) => state.fetchDataDeletionRequests);
  const fetchAllStudents = useAdminStore((state) => state.fetchAllStudents);
  const approveDataDeletionRequest = useAdminStore((state) => state.approveDataDeletionRequest);

  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [approvingId, setApprovingId] = useState<string | null>(null);

  useEffect(() => {
    fetchAllStudents().catch(() => {});
    fetchDataDeletionRequests()
      .catch(() => setError("So'rovlar ro'yxatini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchAllStudents, fetchDataDeletionRequests]);

  async function handleApprove(id: string) {
    setApprovingId(id);
    setError(null);
    try {
      await approveDataDeletionRequest(id);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "So'rovni tasdiqlab bo'lmadi.");
    } finally {
      setApprovingId(null);
    }
  }

  function studentName(id: string) {
    const student = students.find((s) => s.id === id);
    return student ? `${student.firstName} ${student.lastName}` : id;
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <PageHeader
          title="O'chirish so'rovlari"
          description="Ota-onalar tomonidan yuborilgan ma'lumotlarni o'chirish so'rovlarini ko'rib chiqing va tasdiqlang."
          eyebrow="Maxfiylik"
        />

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <ShieldOff className="size-4" strokeWidth={1.75} />
              Kutilayotgan so&apos;rovlar
            </CardTitle>
            <CardDescription>
              Tasdiqlash o&apos;quvchining yozuv (handwriting) profilini va psixologik
              signallarning tafsilotlarini butunlay o&apos;chiradi — bu amalni bekor qilib
              bo&apos;lmaydi.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {error ? <p className="mb-3 text-sm text-destructive">{error}</p> : null}
            {isLoading ? (
              <div className="space-y-2">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : requests.length === 0 ? (
              <EmptyState
                icon={ShieldOff}
                title="Hozircha so'rovlar yo'q"
                description="Ota-onalar tomonidan yuborilgan o'chirish so'rovlari shu yerda ko'rinadi."
              />
            ) : (
              <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground">
                  <tr className="border-b border-border">
                    <th className="py-2 font-medium">O&apos;quvchi</th>
                    <th className="py-2 font-medium">So&apos;ragan foydalanuvchi</th>
                    <th className="py-2 font-medium">Sana</th>
                    <th className="py-2 font-medium">Holati</th>
                    <th className="py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {requests.map((request) => (
                    <tr key={request.id} className="border-b border-border last:border-0">
                      <td className="py-2.5 font-medium">{studentName(request.studentId)}</td>
                      <td className="py-2.5 font-data text-muted-foreground">
                        {request.requestedBy}
                      </td>
                      <td className="py-2.5 font-data text-muted-foreground">
                        {new Date(request.requestedAt).toLocaleDateString("uz-UZ")}
                      </td>
                      <td className="py-2.5">
                        <Badge variant={request.status === "PENDING" ? "flagged" : "secondary"}>
                          {STATUS_LABEL[request.status] ?? request.status}
                        </Badge>
                      </td>
                      <td className="py-2.5 text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={approvingId === request.id || request.status !== "PENDING"}
                          onClick={() => handleApprove(request.id)}
                        >
                          {approvingId === request.id ? "..." : "Tasdiqlash"}
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardShell>
  );
}
