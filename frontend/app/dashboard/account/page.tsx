"use client";

import { useEffect, useState, type FormEvent } from "react";
import { CheckCircle2, KeyRound, Send, UserRound } from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { ROLE_ACCENT_CLASSES } from "@/components/shared/nav-config";
import { useAuthStore } from "@/stores/useAuthStore";
import { useTelegramStore } from "@/stores/useTelegramStore";
import type { ApiErrorResponse, Role } from "@/types/auth";

const ROLE_LABEL: Record<Role, string> = {
  ADMIN: "Administrator",
  TEACHER: "O'qituvchi",
  STUDENT: "O'quvchi",
  PARENT: "Ota-ona",
  PSYCHOLOGIST: "Psixolog",
};

// Not under any role-prefixed folder (/dashboard/{admin,teacher,...}/) — proxy.ts's
// route-guard only enforces role-prefix matching for those literal segments, so this page
// works for whichever role is currently logged in, driven off useAuthStore.user.role at
// render time rather than a hardcoded role prop. One page for every role's self-service
// management: profile info, password, Telegram — role-specific data stays on the dashboards.
export default function AccountPage() {
  const user = useAuthStore((state) => state.user);
  const role: Role = user?.role ?? "STUDENT";

  return (
    <DashboardShell role={role}>
      <div className="space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Profil sozlamalari</h2>
          <p className="text-sm text-muted-foreground">
            Shaxsiy ma&apos;lumotlaringiz, parolingiz va bildirishnoma ulanishlari.
          </p>
        </div>

        <div className="stagger-rise grid max-w-4xl gap-6 lg:grid-cols-2">
          <ProfileCard role={role} />
          <div className="space-y-6">
            <PasswordCard />
            <TelegramCard />
          </div>
        </div>
      </div>
    </DashboardShell>
  );
}

function ProfileCard({ role }: { role: Role }) {
  const profile = useAuthStore((state) => state.profile);
  const fetchProfile = useAuthStore((state) => state.fetchProfile);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    fetchProfile().catch(() => setLoadError("Profil ma'lumotlarini yuklab bo'lmadi."));
  }, [fetchProfile]);

  return (
    <Card className="self-start">
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <UserRound className="size-4" strokeWidth={1.75} />
          Shaxsiy ma&apos;lumotlar
        </CardTitle>
      </CardHeader>
      <CardContent>
        {loadError ? (
          <p className="text-sm text-destructive">{loadError}</p>
        ) : !profile ? (
          <p className="text-sm text-muted-foreground">Yuklanmoqda...</p>
        ) : (
          // Keyed remount seeds the form's local state from the freshly-fetched profile
          // without a setState-in-effect (react-hooks/set-state-in-effect).
          <ProfileForm key={profile.id} role={role} />
        )}
      </CardContent>
    </Card>
  );
}

function ProfileForm({ role }: { role: Role }) {
  const profile = useAuthStore((state) => state.profile);
  const updateProfile = useAuthStore((state) => state.updateProfile);

  const [firstName, setFirstName] = useState(profile?.firstName ?? "");
  const [lastName, setLastName] = useState(profile?.lastName ?? "");
  const [email, setEmail] = useState(profile?.email ?? "");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaved(false);
    setIsSubmitting(true);
    try {
      await updateProfile(firstName, lastName, email);
      setSaved(true);
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Profilni saqlab bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <div className="flex items-center gap-2">
        <Badge variant={ROLE_ACCENT_CLASSES[role].badge}>{ROLE_LABEL[role]}</Badge>
        {profile ? (
          <span className="font-data text-sm text-muted-foreground">{profile.phone}</span>
        ) : null}
      </div>
      <FormField label="Ism" htmlFor="firstName">
        <input
          id="firstName"
          value={firstName}
          onChange={(e) => setFirstName(e.target.value)}
          required
          className={`${fieldClass} w-full`}
        />
      </FormField>
      <FormField label="Familiya" htmlFor="lastName">
        <input
          id="lastName"
          value={lastName}
          onChange={(e) => setLastName(e.target.value)}
          required
          className={`${fieldClass} w-full`}
        />
      </FormField>
      <FormField label="Email (ixtiyoriy — parolni tiklash uchun ishlatiladi)" htmlFor="email">
        <input
          id="email"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={`${fieldClass} w-full`}
        />
      </FormField>
      <p className="text-xs text-muted-foreground">
        Telefon raqam tizimga kirish uchun ishlatiladi va o&apos;zgartirilmaydi.
      </p>
      {saved ? (
        <p className="flex items-center gap-1.5 text-sm text-success">
          <CheckCircle2 className="size-3.5" strokeWidth={1.75} />
          Profil saqlandi.
        </p>
      ) : null}
      {error ? <p className="text-sm text-destructive">{error}</p> : null}
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Saqlanmoqda..." : "Saqlash"}
      </Button>
    </form>
  );
}

function PasswordCard() {
  const changePassword = useAuthStore((state) => state.changePassword);

  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const mismatch = confirmPassword.length > 0 && newPassword !== confirmPassword;

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSaved(false);

    if (newPassword !== confirmPassword) {
      setError("Yangi parollar bir xil emas.");
      return;
    }

    setIsSubmitting(true);
    try {
      await changePassword(oldPassword, newPassword);
      setSaved(true);
      setOldPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } }).response?.data;
      setError(apiError?.message ?? "Parolni o'zgartirib bo'lmadi.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <KeyRound className="size-4" strokeWidth={1.75} />
          Parolni o&apos;zgartirish
        </CardTitle>
        {saved ? (
          <CardDescription className="flex items-center gap-1.5 text-success">
            <CheckCircle2 className="size-3.5" strokeWidth={1.75} />
            Parol muvaffaqiyatli o&apos;zgartirildi.
          </CardDescription>
        ) : null}
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-3">
          <FormField label="Joriy parol" htmlFor="oldPassword">
            <input
              id="oldPassword"
              type="password"
              autoComplete="current-password"
              value={oldPassword}
              onChange={(e) => setOldPassword(e.target.value)}
              required
              className={`${fieldClass} w-full`}
            />
          </FormField>
          <FormField label="Yangi parol" htmlFor="newPassword">
            <input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              required
              className={`${fieldClass} w-full`}
            />
          </FormField>
          <FormField label="Yangi parolni tasdiqlang" htmlFor="confirmPassword">
            <input
              id="confirmPassword"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              required
              aria-invalid={mismatch}
              className={`${fieldClass} w-full`}
            />
          </FormField>
          {mismatch ? (
            <p className="text-sm text-destructive">Yangi parollar bir xil emas.</p>
          ) : null}
          {error ? <p className="text-sm text-destructive">{error}</p> : null}
          <Button type="submit" disabled={isSubmitting || mismatch}>
            {isSubmitting ? "Saqlanmoqda..." : "Parolni yangilash"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

function TelegramCard() {
  const status = useTelegramStore((state) => state.status);
  const pendingLink = useTelegramStore((state) => state.pendingLink);
  const isLoading = useTelegramStore((state) => state.isLoading);
  const fetchStatus = useTelegramStore((state) => state.fetchStatus);
  const generateLinkToken = useTelegramStore((state) => state.generateLinkToken);
  const unlink = useTelegramStore((state) => state.unlink);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Send className="size-4" strokeWidth={1.75} />
          Telegram bildirishnomalari
        </CardTitle>
      </CardHeader>
      <CardContent className="text-sm">
        {!status ? (
          <p className="text-muted-foreground">Yuklanmoqda...</p>
        ) : status.connected ? (
          <div className="flex flex-col items-start gap-3">
            <p className="flex items-center gap-1.5 text-muted-foreground">
              <span className="size-2 rounded-full bg-success" aria-hidden />
              Ulangan{status.telegramUsername ? `: @${status.telegramUsername}` : ""}
            </p>
            <Button variant="outline" size="sm" onClick={unlink}>
              Uzish
            </Button>
          </div>
        ) : pendingLink ? (
          <div className="flex flex-col items-start gap-3">
            <p className="text-muted-foreground">
              Havolani Telegram&apos;da oching va botni ishga tushiring (5 daqiqa amal qiladi):
            </p>
            <a
              href={pendingLink.linkUrl}
              target="_blank"
              rel="noreferrer"
              className="break-all text-primary underline underline-offset-4"
            >
              {pendingLink.linkUrl}
            </a>
            <Button variant="outline" size="sm" onClick={fetchStatus}>
              Ulanganini tekshirish
            </Button>
          </div>
        ) : (
          <div className="flex flex-col items-start gap-3">
            <p className="text-muted-foreground">
              Bildirishnomalarni Telegram orqali olish uchun hisobingizni botga ulang.
            </p>
            <Button size="sm" onClick={generateLinkToken} disabled={isLoading}>
              Telegram&apos;ni ulash
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
