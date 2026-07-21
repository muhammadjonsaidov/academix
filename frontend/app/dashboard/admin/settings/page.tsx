"use client";

import { useEffect, useState } from "react";
import { CheckCircle2, Gauge, School, Settings as SettingsIcon } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminStore } from "@/stores/useAdminStore";

export default function AdminSettingsPage() {
  const school = useAdminStore((state) => state.school);
  const fetchSchool = useAdminStore((state) => state.fetchSchool);
  const updateSchool = useAdminStore((state) => state.updateSchool);

  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [region, setRegion] = useState("");
  const [district, setDistrict] = useState("");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    fetchSchool()
      .then(() => {
        const loaded = useAdminStore.getState().school;
        if (loaded) {
          setName(loaded.name);
          setAddress(loaded.address);
          setRegion(loaded.region);
          setDistrict(loaded.district);
          setPhone(loaded.phone ?? "");
        }
      })
      .catch(() => setError("Maktab ma'lumotlarini yuklab bo'lmadi."))
      .finally(() => setIsLoading(false));
  }, [fetchSchool]);

  async function handleSave() {
    setError(null);
    setSaved(false);
    setIsSaving(true);
    try {
      await updateSchool({ name, address, region, district, phone: phone || null });
      setSaved(true);
    } catch {
      setError("Saqlab bo'lmadi.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <DashboardShell role="ADMIN">
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Maktab sozlamalari</h2>
          <p className="text-sm text-muted-foreground">
            Maktab ma&apos;lumotlarini va AI foydalanish limitini ko&apos;ring.
          </p>
        </div>

        {error ? <p className="text-sm text-destructive">{error}</p> : null}

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <SettingsIcon className="size-4" strokeWidth={1.75} />
              Ma&apos;lumotlar
            </CardTitle>
            {saved ? (
              <CardDescription className="flex items-center gap-1.5 text-success">
                <CheckCircle2 className="size-3.5" strokeWidth={1.75} />
                Saqlandi.
              </CardDescription>
            ) : null}
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="max-w-md space-y-3">
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
                <Skeleton className="h-9 w-full" />
              </div>
            ) : school ? (
              <div className="max-w-md space-y-3">
                <FormField label="Nomi" htmlFor="name">
                  <input
                    id="name"
                    className={`${fieldClass} w-full`}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                  />
                </FormField>
                <FormField label="Manzil" htmlFor="address">
                  <input
                    id="address"
                    className={`${fieldClass} w-full`}
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                  />
                </FormField>
                <FormField label="Viloyat" htmlFor="region">
                  <input
                    id="region"
                    className={`${fieldClass} w-full`}
                    value={region}
                    onChange={(e) => setRegion(e.target.value)}
                  />
                </FormField>
                <FormField label="Tuman" htmlFor="district">
                  <input
                    id="district"
                    className={`${fieldClass} w-full`}
                    value={district}
                    onChange={(e) => setDistrict(e.target.value)}
                  />
                </FormField>
                <FormField label="Telefon" htmlFor="phone">
                  <input
                    id="phone"
                    className={`${fieldClass} w-full`}
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                  />
                </FormField>
                <Button onClick={handleSave} disabled={isSaving}>
                  {isSaving ? "Saqlanmoqda..." : "Saqlash"}
                </Button>
              </div>
            ) : null}
          </CardContent>
        </Card>

        {school ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <Card>
              <CardContent className="flex items-center gap-4 py-5">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-role-admin-muted text-role-admin">
                  <Gauge className="size-5" strokeWidth={1.75} />
                </span>
                <div>
                  <p className="font-data text-2xl leading-none font-semibold">
                    {school.currentMonthAiUsage} / {school.monthlyAiCallLimit}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">AI limiti (bu oy)</p>
                </div>
              </CardContent>
            </Card>
            <Card>
              <CardContent className="flex items-center gap-4 py-5">
                <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-role-admin-muted text-role-admin">
                  <School className="size-5" strokeWidth={1.75} />
                </span>
                <div>
                  <p className="font-data text-2xl leading-none font-semibold">
                    {school.totalClasses}
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">Jami sinflar</p>
                </div>
              </CardContent>
            </Card>
          </div>
        ) : null}
      </div>
    </DashboardShell>
  );
}
