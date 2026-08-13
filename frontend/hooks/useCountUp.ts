"use client";

import { useEffect, useState } from "react";
import { useInView } from "@/hooks/useInView";

/**
 * Counts `target` up from 0 with a cubic ease-out once the element it's bound
 * to scrolls into view — the signature "numbers come alive" moment on landing
 * stats and dashboard stat tiles. Returns `ref` to attach to the element that
 * should trigger the animation and `value` (float; round at the call site).
 * Reduced motion: jumps straight to `target`.
 */
export function useCountUp(target: number, { duration = 1100 }: { duration?: number } = {}) {
  const { ref, inView } = useInView<HTMLSpanElement>();
  const [value, setValue] = useState(0);

  useEffect(() => {
    if (!inView) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      // Deferred via rAF — a synchronous setState in the effect body trips the
      // react-hooks/set-state-in-effect rule (and reads better anyway).
      const raf = requestAnimationFrame(() => setValue(target));
      return () => cancelAnimationFrame(raf);
    }
    let raf = 0;
    const start = performance.now();
    const tick = (now: number) => {
      const progress = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      setValue(target * eased);
      if (progress < 1) raf = requestAnimationFrame(tick);
    };
    raf = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(raf);
  }, [inView, target, duration]);

  return { ref, value };
}
