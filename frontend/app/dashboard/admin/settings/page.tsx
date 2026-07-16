"use client";

import { useEffect, useState } from "react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { AdminNav } from "@/components/admin/AdminNav";
import { Button } from "@/components/ui/button";
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
      .catch(() => setError("Maktab ma'lumotlarini yuklab bo'lmadi."));
  }, [fetchSchool]);

  async function handleSave() {
    setError(null);
    setSaved(false);
    try {
      await updateSchool({ name, address, region, district, phone: phone || null });
      setSaved(true);
    } catch {
      setError("Saqlab bo'lmadi.");
    }
  }

  if (!school) {
    return (
      <DashboardShell role="ADMIN">
        <AdminNav />
        {error ? <p className="text-sm text-destructive">{error}</p> : null}
      </DashboardShell>
    );
  }

  return (
    <DashboardShell role="ADMIN">
      <AdminNav />
      <h2 className="mb-4 text-lg font-semibold">Maktab sozlamalari</h2>

      {error ? <p className="mb-4 text-sm text-destructive">{error}</p> : null}
      {saved ? <p className="mb-4 text-sm text-green-600">Saqlandi.</p> : null}

      <div className="max-w-md space-y-3">
        <div>
          <label className="mb-1 block text-xs text-muted-foreground">Nomi</label>
          <input
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <div>
          <label className="mb-1 block text-xs text-muted-foreground">Manzil</label>
          <input
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
          />
        </div>
        <div>
          <label className="mb-1 block text-xs text-muted-foreground">Viloyat</label>
          <input
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            value={region}
            onChange={(e) => setRegion(e.target.value)}
          />
        </div>
        <div>
          <label className="mb-1 block text-xs text-muted-foreground">Tuman</label>
          <input
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            value={district}
            onChange={(e) => setDistrict(e.target.value)}
          />
        </div>
        <div>
          <label className="mb-1 block text-xs text-muted-foreground">Telefon</label>
          <input
            className="w-full rounded-md border border-border bg-background px-3 py-2 text-sm"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />
        </div>
        <Button onClick={handleSave}>Saqlash</Button>
      </div>

      <div className="mt-6 rounded-md border border-border p-4 text-sm text-muted-foreground">
        <p>AI limiti: {school.currentMonthAiUsage} / {school.monthlyAiCallLimit}</p>
        <p>Jami sinflar: {school.totalClasses}</p>
      </div>
    </DashboardShell>
  );
}
