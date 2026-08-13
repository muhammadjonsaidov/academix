"use client";

import { useInView } from "@/hooks/useInView";
import { cn } from "@/lib/utils";

interface RevealProps {
  children: React.ReactNode;
  /** Stagger delay in ms — pass `index * step` for card-grid cascades. */
  delay?: number;
  className?: string;
}

/**
 * Scroll-reveal wrapper — children settle up-and-in (the system's "ink settles
 * onto paper" rise) the first time they enter the viewport. Delays are applied
 * only once visible, so off-screen cards are never held back by a queued delay.
 */
export function Reveal({ children, delay = 0, className }: RevealProps) {
  const { ref, inView } = useInView<HTMLDivElement>();
  return (
    <div
      ref={ref}
      className={cn("reveal", inView && "is-visible", className)}
      style={inView ? { transitionDelay: `${delay}ms` } : undefined}
    >
      {children}
    </div>
  );
}
