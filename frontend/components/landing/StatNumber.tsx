"use client";

import { useCountUp } from "@/hooks/useCountUp";

export function StatNumber({ raw }: { raw: string }) {
  const match = /^(\d+)(.*)$/.exec(raw);
  const { ref, value } = useCountUp(match ? Number(match[1]) : 0);
  if (!match) return <>{raw}</>;
  return <span ref={ref}>{Math.round(value)}{match[2]}</span>;
}
