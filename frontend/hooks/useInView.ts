"use client";

import { useEffect, useRef, useState } from "react";

/**
 * Observes an element and flips `inView` to true the first time it enters the
 * viewport (then disconnects — one-shot by design). Powers the scroll-reveal
 * system (Reveal, ProgressBar fill, count-up). Reduced motion: reveals
 * immediately instead of waiting for the observer, so the page is calm for
 * users who ask for it.
 */
export function useInView<T extends HTMLElement = HTMLElement>(
  options?: IntersectionObserverInit,
) {
  const ref = useRef<T>(null);
  const [inView, setInView] = useState(false);

  useEffect(() => {
    const element = ref.current;
    if (!element) return;

    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      // Deferred via rAF — a synchronous setState in the effect body trips the
      // react-hooks/set-state-in-effect rule.
      const raf = requestAnimationFrame(() => setInView(true));
      return () => cancelAnimationFrame(raf);
    }

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setInView(true);
          observer.disconnect();
        }
      },
      { rootMargin: "0px 0px -10% 0px", threshold: 0.15, ...options },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [options]);

  return { ref, inView };
}
