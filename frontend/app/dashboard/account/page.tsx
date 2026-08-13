"use client";

import { useEffect, useState, type FormEvent } from "react";
import {
  BellRing,
  CheckCircle2,
  Gauge,
  KeyRound,
  Lock,
  School,
  Send,
  UserRound,
} from "lucide-react";
import { DashboardShell } from "@/components/shared/DashboardShell";
import { fieldClass, FormField } from "@/components/shared/FormField";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ROLE_ACCENT_CLASSES } from "@/components/shared/nav-config";
import { useAdminStore } from "@/stores/useAdminStore";
import { useAuthStore } from "@/stores/useAuthStore";
import { useNotificationStore } from "@/stores/useNotificationStore";
import { useTelegramStore } from "@/stores/useTelegramStore";
import type { ApiErrorResponse, Role } from "@/types/auth";
import type { NotificationType } from "@/types/notification";

const ROLE_LABEL: Record<Role, string> = {
  ADMIN: "Administrator",
  TEACHER: "O'qituvchi",
  STUDENT: "O'quvchi",
  PARENT: "Ota-ona",
  PSYCHOLOGIST: "Psixolog",
};

type SettingsSection = "profile" | "school" | "security" | "notifications" | "telegram";

interface SectionDef {
  key: SettingsSection;
  label: string;
  description: string;
  icon: typeof UserRound;
  /** Restrict a section to specific roles — omitted means every role sees it. */
  roles?: Role[];
}

const SECTIONS: SectionDef[] = [
  {
    key: "profile",
    label: "Profil",
    description: "Ism, familiya va email manzilingiz.",
    icon: UserRound,
  },
  {
    key: "school",
    label: "Muassasa",
    description: "Ta'lim muassasasi ma'lumotlari va AI foydalanish limiti.",
    icon: School,
    roles: ["ADMIN"],
  },
  {
    key: "security",
    label: "Xavfsizlik",
    description: "Hisobingiz parolini boshqaring.",
    icon: KeyRound,
  },
  {
    key: "notifications",
    label: "Bildirishnomalar",
    description: "Qaysi bildirishnoma qaysi kanaldan kelishini tanlang.",
    icon: BellRing,
  },
  {
    key: "telegram",
    label: "Telegram",
    description: "Bildirishnomalar uchun Telegram botni ulang.",
    icon: Send,
  },
];

// Not under any role-prefixed folder (/dashboard/{admin,teacher,...}/) — proxy.ts's
// route-guard only enforces role-prefix matching for those literal segments, so this page
// works for whichever role is currently logged in, driven off useAuthStore.user.role at
// render time rather than a hardcoded role prop. One page for every role's self-service
// management, laid out as a standard settings screen: a section nav on the left, one
// section's content at a time on the right (top tabs on mobile).
export default function AccountPage() {
  const user = useAuthStore((state) => state.user);
  const role: Role = user?.role ?? "STUDENT";
  const [section, setSection] = useState<SettingsSection>("profile");

  const visibleSections = SECTIONS.filter((s) => !s.roles || s.roles.includes(role));
  const active = visibleSections.find((s) => s.key === section) ?? visibleSections[0];

  return (
    <DashboardShell role={role}>
      <div className="mx-auto max-w-4xl space-y-6">
        <div>
          <h2 className="font-heading text-xl font-semibold">Sozlamalar</h2>
          <p className="text-sm text-muted-foreground">
            Hisobingiz va bildirishnomalarni boshqaring.
          </p>
        </div>

        <div className="flex flex-col gap-6 lg:flex-row lg:gap-10">
          {/* Section nav — vertical on desktop, horizontal scroll strip on mobile */}
          <nav className="flex shrink-0 gap-1 overflow-x-auto lg:w-52 lg:flex-col lg:overflow-visible">
            {visibleSections.map((s) => {
              const Icon = s.icon;
              const isActive = s.key === active.key;
              return (
                <button
                  key={s.key}
                  type="button"
                  onClick={() => setSection(s.key)}
                  aria-current={isActive ? "page" : undefined}
                  className={cnSection(isActive)}
                >
                  <Icon className="size-4 shrink-0" strokeWidth={1.75} />
                  {s.label}
                </button>
              );
            })}
          </nav>

          {/* Active section content — keyed so switching re-runs the rise animation */}
          <div key={active.key} className="animate-rise min-w-0 flex-1">
            <div className="mb-4">
              <h3 className="font-heading text-base font-semibold">{active.label}</h3>
              <p className="text-sm text-muted-foreground">{active.description}</p>
            </div>
            {active.key === "profile" ? <ProfileCard role={role} /> : null}
            {active.key === "school" ? <SchoolCard /> : null}
            {active.key === "security" ? <PasswordCard /> : null}
            {active.key === "notifications" ? <NotificationPreferencesCard /> : null}
            {active.key === "telegram" ? <TelegramCard /> : null}
          </div>
        </div>
      </div>
    </DashboardShell>
  );
}

function cnSection(isActive: boolean): string {
  return [
    "flex items-center gap-2.5 rounded-md border-l-2 border-transparent px-3 py-2 text-sm font-medium whitespace-nowrap transition-colors outline-none focus-visible:ring-3 focus-visible:ring-ring/50",
    isActive
      ? "border-foreground bg-accent text-foreground"
      : "text-muted-foreground hover:bg-muted/60 hover:text-foreground",
  ].join(" ");
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
      <CardContent className="py-6">
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
      <CardContent className="py-6">
        <form onSubmit={handleSubmit} className="space-y-3">
          {saved ? (
            <p className="flex items-center gap-1.5 text-sm text-success">
              <CheckCircle2 className="size-3.5" strokeWidth={1.75} />
              Parol muvaffaqiyatli o&apos;zgartirildi.
            </p>
          ) : null}
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

// ADMIN-only "Muassasa" section — moved wholesale from the old /dashboard/admin/settings page
// (that route now redirects here). Same useAdminStore school fetch/update wiring.
function SchoolCard() {
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
      .catch(() => setError("Muassasa ma'lumotlarini yuklab bo'lmadi."))
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
    <div className="space-y-4">
      <Card>
        <CardContent className="py-6">
          {isLoading ? (
            <div className="space-y-3">
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
              <Skeleton className="h-9 w-full" />
            </div>
          ) : school ? (
            <div className="space-y-3">
              <FormField label="Nomi" htmlFor="schoolName">
                <input
                  id="schoolName"
                  className={`${fieldClass} w-full`}
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </FormField>
              <FormField label="Manzil" htmlFor="schoolAddress">
                <input
                  id="schoolAddress"
                  className={`${fieldClass} w-full`}
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                />
              </FormField>
              <FormField label="Viloyat" htmlFor="schoolRegion">
                <input
                  id="schoolRegion"
                  className={`${fieldClass} w-full`}
                  value={region}
                  onChange={(e) => setRegion(e.target.value)}
                />
              </FormField>
              <FormField label="Tuman" htmlFor="schoolDistrict">
                <input
                  id="schoolDistrict"
                  className={`${fieldClass} w-full`}
                  value={district}
                  onChange={(e) => setDistrict(e.target.value)}
                />
              </FormField>
              <FormField label="Telefon" htmlFor="schoolPhone">
                <input
                  id="schoolPhone"
                  className={`${fieldClass} w-full`}
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                />
              </FormField>
              {saved ? (
                <p className="flex items-center gap-1.5 text-sm text-success">
                  <CheckCircle2 className="size-3.5" strokeWidth={1.75} />
                  Saqlandi.
                </p>
              ) : null}
              {error ? <p className="text-sm text-destructive">{error}</p> : null}
              <Button onClick={handleSave} disabled={isSaving}>
                {isSaving ? "Saqlanmoqda..." : "Saqlash"}
              </Button>
            </div>
          ) : error ? (
            <p className="text-sm text-destructive">{error}</p>
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
  );
}

const NOTIFICATION_TYPE_LABEL: Record<NotificationType, string> = {
  HOMEWORK_ASSIGNED: "Yangi uy vazifasi",
  DEADLINE_REMINDER: "Muddat eslatmasi",
  HOMEWORK_GRADED: "Baholangan ishlar",
  STREAK_BROKEN: "Faollik uzilishi",
  STREAK_MILESTONE: "Faollik yutuqlari",
  BADGE_EARNED: "Yutuq belgilari",
  PSYCHOLOGICAL_ALERT: "Psixologik signallar",
  LATE_SUBMISSION: "Kechikkan topshiriqlar",
  CLASS_PROGRESS_REPORT: "Sinf hisobotlari",
  HANDWRITING_PROFILE_RESET: "Qo'lyozma profili qayta tiklanishi",
  AI_BUDGET_LOW: "AI limiti ogohlantirishi",
};

function NotificationPreferencesCard() {
  const preferences = useNotificationStore((state) => state.preferences);
  const fetchPreferences = useNotificationStore((state) => state.fetchPreferences);
  const updatePreference = useNotificationStore((state) => state.updatePreference);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchPreferences().catch(() => setError("Sozlamalarni yuklab bo'lmadi."));
  }, [fetchPreferences]);

  async function handleToggle(
    type: NotificationType,
    inAppEnabled: boolean,
    telegramEnabled: boolean,
  ) {
    setError(null);
    try {
      // In-app is the base channel (the inbox row is what Telegram delivery hangs off) —
      // turning it off turns Telegram off with it, mirroring the backend's send logic.
      await updatePreference(type, inAppEnabled, inAppEnabled ? telegramEnabled : false);
    } catch {
      setError("Sozlamani saqlab bo'lmadi.");
    }
  }

  return (
    <Card>
      <CardContent className="py-6">
        {preferences.length === 0 && !error ? (
          <p className="text-sm text-muted-foreground">Yuklanmoqda...</p>
        ) : (
          <div className="space-y-1">
            <div className="flex items-center justify-end gap-4 pb-1 text-xs font-medium text-muted-foreground">
              <span className="w-12 text-center">Ilova</span>
              <span className="w-12 text-center">Telegram</span>
            </div>
            {preferences.map((pref) => (
              <div
                key={pref.type}
                className="flex items-center justify-between gap-3 rounded-md py-1.5 text-sm"
              >
                <span className="flex min-w-0 items-center gap-1.5">
                  <span className="truncate">{NOTIFICATION_TYPE_LABEL[pref.type] ?? pref.type}</span>
                  {pref.locked && (
                    <Lock
                      className="size-3 shrink-0 text-muted-foreground"
                      strokeWidth={1.75}
                      aria-label="Har doim yuboriladi"
                    />
                  )}
                </span>
                <span className="flex items-center gap-4">
                  <span className="flex w-12 justify-center">
                    <input
                      type="checkbox"
                      className="size-4 accent-[var(--ink)]"
                      checked={pref.inAppEnabled}
                      disabled={pref.locked}
                      aria-label={`${NOTIFICATION_TYPE_LABEL[pref.type]} — ilova ichida`}
                      onChange={(e) =>
                        handleToggle(pref.type, e.target.checked, pref.telegramEnabled)
                      }
                    />
                  </span>
                  <span className="flex w-12 justify-center">
                    <input
                      type="checkbox"
                      className="size-4 accent-[var(--ink)]"
                      checked={pref.telegramEnabled}
                      disabled={pref.locked || !pref.inAppEnabled}
                      aria-label={`${NOTIFICATION_TYPE_LABEL[pref.type]} — Telegram`}
                      onChange={(e) =>
                        handleToggle(pref.type, pref.inAppEnabled, e.target.checked)
                      }
                    />
                  </span>
                </span>
              </div>
            ))}
            <p className="pt-2 text-xs text-muted-foreground">
              Psixologik signallar xavfsizlik qoidasi bo&apos;yicha doim yuboriladi. Ilova kanali
              o&apos;chirilsa, Telegram ham o&apos;chadi.
            </p>
          </div>
        )}
        {error ? <p className="mt-2 text-sm text-destructive">{error}</p> : null}
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
      <CardContent className="py-6 text-sm">
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
