"use client";

import { useInView } from "@/hooks/useInView";
import { cn } from "@/lib/utils";

interface RevealProps {
  children: React.ReactNode;
  /** Stagger delay in ms — pass `index * step` for card-grid cascades. */
  delay?: number;
  className?: string;
  as?: "div" | "li";
}

/**
 * Scroll-reveal wrapper — children settle up-and-in (the system's "ink settles
 * onto paper" rise) the first time they enter the viewport. Delays are applied
 * only once visible, so off-screen cards are never held back by a queued delay.
 */
export function Reveal({ children, delay = 0, className, as = "div" }: RevealProps) {
  const { ref, inView } = useInView<HTMLDivElement>();
  const classNameValue = cn("reveal", inView && "is-visible", className);
  const style = inView ? { transitionDelay: `${delay}ms` } : undefined;
  return as === "li" ? (
    <li
      ref={ref as unknown as React.RefObject<HTMLLIElement>}
      className={classNameValue}
      style={style}
    >
      {children}
    </li>
  ) : (
    <div ref={ref} className={classNameValue} style={style}>
      {children}
    </div>
  );
}
