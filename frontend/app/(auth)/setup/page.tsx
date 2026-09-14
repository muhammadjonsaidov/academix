"use client";

import { useState, type FormEvent } from "react";
import axios from "axios";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { ArrowLeft, CheckCircle2, Sparkles } from "lucide-react";
import { fieldClass } from "@/components/shared/FormField";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { validatePassword } from "@/lib/password-policy";
import { useAuthStore } from "@/stores/useAuthStore";
import type { ApiErrorResponse } from "@/types/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "/api/v1";

type FormState = {
  firstName: string;
  lastName: string;
  phone: string;
  email: string;
  password: string;
  confirmPassword: string;
  schoolName: string;
  region: string;
  district: string;
  address: string;
};

const initialState: FormState = {
  firstName: "",
  lastName: "",
  phone: "",
  email: "",
  password: "",
  confirmPassword: "",
  schoolName: "",
  region: "",
  district: "",
  address: "",
};

/** One-time setup screen. The backend enforces the single-use rule; this page only makes it clear. */
export default function InitialSetupPage() {
  const router = useRouter();
  const login = useAuthStore((state) => state.login);
  const [form, setForm] = useState<FormState>(initialState);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  function setField(field: keyof FormState, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    const passwordResult = validatePassword(form.password);
    if (!passwordResult.valid) {
      setError(passwordResult.error);
      return;
    }
    if (form.password !== form.confirmPassword) {
      setError("Parollar bir xil emas.");
      return;
    }
    setIsSubmitting(true);
    try {
      await axios.post(`${API_BASE_URL}/onboarding/initial-setup`, {
        firstName: form.firstName,
        lastName: form.lastName,
        phone: form.phone,
        email: form.email,
        password: form.password,
        schoolName: form.schoolName,
        region: form.region,
        district: form.district,
        address: form.address,
      });
      // Perform the standard login once so the server sets both secure httpOnly cookies.
      await login(form.phone, form.password);
      router.replace("/dashboard/admin");
    } catch (err) {
      const apiError = (err as { response?: { data?: ApiErrorResponse } })
        .response?.data;
      setError(
        apiError?.message ??
          "Sozlashni yakunlab bo'lmadi. Qayta urinib ko'ring.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="bg-grid-paper relative isolate flex min-h-screen items-center justify-center p-4 py-8">
      <div
        aria-hidden
        className="bg-ai-gradient absolute top-0 left-1/2 -z-10 size-[32rem] -translate-x-1/2 rounded-full opacity-15 blur-3xl"
      />
      <Card className="animate-rise w-full max-w-2xl shadow-lg">
        <CardHeader className="space-y-3">
          <Link
            href="/login"
            className="inline-flex w-fit items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-4" /> Kirishga qaytish
          </Link>
          <div className="flex items-start gap-3">
            <Image
              src="/logo-mark.svg"
              alt="AcademiX AI"
              width={46}
              height={43}
              priority
            />
            <div>
              <p className="flex items-center gap-1.5 text-sm font-medium text-ai">
                <Sparkles className="size-4" /> Birinchi sozlash
              </p>
              <h1 className="font-heading text-2xl font-semibold">
                Maktabingizni ishga tushiring
              </h1>
              <p className="mt-1 text-sm text-muted-foreground">
                Bu sahifa faqat bir marta administrator va maktab profilini
                yaratadi.
              </p>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <fieldset className="space-y-3">
              <legend className="mb-2 text-sm font-semibold">
                Administrator hisobi
              </legend>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field
                  label="Ism"
                  value={form.firstName}
                  onChange={(value) => setField("firstName", value)}
                  autoComplete="given-name"
                />
                <Field
                  label="Familiya"
                  value={form.lastName}
                  onChange={(value) => setField("lastName", value)}
                  autoComplete="family-name"
                />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field
                  label="Telefon raqam"
                  value={form.phone}
                  onChange={(value) => setField("phone", value)}
                  type="tel"
                  placeholder="+998901234567"
                  autoComplete="tel"
                />
                <Field
                  label="Email (ixtiyoriy)"
                  value={form.email}
                  onChange={(value) => setField("email", value)}
                  type="email"
                  autoComplete="email"
                  required={false}
                />
              </div>
              <div className="grid gap-3 sm:grid-cols-2">
                <Field
                  label="Parol"
                  value={form.password}
                  onChange={(value) => setField("password", value)}
                  type="password"
                  autoComplete="new-password"
                  hint="8 belgi, katta/kichik harf va raqam"
                />
                <Field
                  label="Parolni tasdiqlang"
                  value={form.confirmPassword}
                  onChange={(value) => setField("confirmPassword", value)}
                  type="password"
                  autoComplete="new-password"
                />
              </div>
            </fieldset>
            <fieldset className="space-y-3 border-t border-border pt-5">
              <legend className="mb-2 text-sm font-semibold">
                Maktab ma&apos;lumotlari
              </legend>
              <Field
                label="Maktab nomi"
                value={form.schoolName}
                onChange={(value) => setField("schoolName", value)}
              />
              <div className="grid gap-3 sm:grid-cols-2">
                <Field
                  label="Viloyat"
                  value={form.region}
                  onChange={(value) => setField("region", value)}
                />
                <Field
                  label="Tuman yoki shahar"
                  value={form.district}
                  onChange={(value) => setField("district", value)}
                />
              </div>
              <Field
                label="Manzil"
                value={form.address}
                onChange={(value) => setField("address", value)}
              />
            </fieldset>
            {error ? (
              <p
                role="alert"
                className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive"
              >
                {error}
              </p>
            ) : null}
            <div className="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                <CheckCircle2 className="size-4 text-success" /> Keyin xodim va
                o&apos;quvchilarni admin paneldan qo&apos;shasiz.
              </p>
              <Button
                type="submit"
                variant="gradient"
                size="lg"
                disabled={isSubmitting}
              >
                {isSubmitting ? "Yaratilmoqda..." : "Maktabni ishga tushirish"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </main>
  );
}

function Field({
  label,
  value,
  onChange,
  type = "text",
  placeholder,
  autoComplete,
  hint,
  required = true,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
  placeholder?: string;
  autoComplete?: string;
  hint?: string;
  required?: boolean;
}) {
  const id = `setup-${label.toLowerCase().replaceAll(" ", "-")}`;
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
        {hint ? (
          <span className="ml-1 font-normal text-muted-foreground">
            ({hint})
          </span>
        ) : null}
      </label>
      <input
        id={id}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required={required}
        placeholder={placeholder}
        autoComplete={autoComplete}
        className={`${fieldClass} w-full`}
      />
    </div>
  );
}
