"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Bot, Camera, RotateCcw, ScanLine, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { InkMark } from "@/components/ui/ink-mark";
import { cn } from "@/lib/utils";

interface DemoStep {
  text: string;
  correct: boolean;
  note?: string;
}

interface SampleProblem {
  question: string;
  steps: DemoStep[];
  grade: number;
  verdict: string;
}

const SAMPLES: SampleProblem[] = [
  {
    question: "24 × 3 + 18 = ?",
    steps: [
      { text: "24 × 3 = 72", correct: true },
      { text: "72 + 18 = 90", correct: true },
    ],
    grade: 5,
    verdict: "Barakalla — ikkala qadam ham to'g'ri.",
  },
  {
    question: "x − 7 = 19;  x = ?",
    steps: [
      { text: "x = 19 + 7", correct: true },
      { text: "x = 26", correct: true },
    ],
    grade: 5,
    verdict: "A'lo — tenglama to'g'ri yechilgan.",
  },
  {
    question: "18 : 6 + 4 × 2 = ?",
    steps: [
      { text: "18 : 6 = 3", correct: true },
      { text: "4 × 2 = 6", correct: false, note: "4 × 2 = 8 bo'lishi kerak" },
      { text: "3 + 8 = 11", correct: true },
    ],
    grade: 4,
    verdict: "Bitta qadam xato — AI tuzatishni ko'rsatdi.",
  },
];

type Stage = "scanning" | "steps" | "done";

// Chip labels are deliberately plain-language ("O'qish", not "OCR") — the
// landing page is read by parents and school directors, not just engineers.
const PIPELINE = [
  { key: "ocr", label: "O'qish", icon: Camera },
  { key: "analysis", label: "Tahlil", icon: Sparkles },
  { key: "grade", label: "Baho", icon: Bot },
] as const;

const SCAN_MS = 1500;
const STEP_REVEAL_MS = 550;
const FINALIZE_MS = 450;

/**
 * Landing-page live demo: picks a sample problem and plays the whole AI
 * grading pipeline in the browser — OCR scan sweep, step-by-step check/cross
 * reveals (InkMark), then the grade stamps in. Pure client-side simulation
 * (no backend calls); the sequence is staged with timers that are always
 * cleaned up. Auto-plays the first sample so the hero feels alive on load.
 */
export function GradingDemo() {
  const [sampleIndex, setSampleIndex] = useState(0);
  const [stage, setStage] = useState<Stage>("scanning");
  const [visibleSteps, setVisibleSteps] = useState(0);
  const timersRef = useRef<number[]>([]);

  const clearTimers = useCallback(() => {
    timersRef.current.forEach((id) => window.clearTimeout(id));
    timersRef.current = [];
  }, []);

  const runSample = useCallback(
    (index: number, delay = 0) => {
      clearTimers();
      setSampleIndex(index);
      setStage("scanning");
      setVisibleSteps(0);

      const schedule = (ms: number, fn: () => void) => {
        timersRef.current.push(window.setTimeout(fn, delay + ms));
      };

      const sample = SAMPLES[index];
      schedule(SCAN_MS, () => setStage("steps"));
      sample.steps.forEach((_, i) => {
        schedule(SCAN_MS + STEP_REVEAL_MS * (i + 1), () => setVisibleSteps(i + 1));
      });
      schedule(
        SCAN_MS + STEP_REVEAL_MS * sample.steps.length + FINALIZE_MS,
        () => setStage("done"),
      );
    },
    [clearTimers],
  );

  // Auto-loop: after the grade stamps in, pause on it, then roll into the next
  // sample so the demo keeps running like a live dashboard instead of going idle.
  useEffect(() => {
    if (stage !== "done") return;
    const id = window.setTimeout(() => runSample((sampleIndex + 1) % SAMPLES.length), 2800);
    return () => window.clearTimeout(id);
  }, [stage, sampleIndex, runSample]);

  useEffect(() => {
    // Deferred inside a timer callback so the state machine never calls
    // setState synchronously in the effect body (react-hooks lint rule).
    const startId = window.setTimeout(() => runSample(0), 900);
    return () => {
      window.clearTimeout(startId);
      clearTimers();
    };
  }, [runSample, clearTimers]);

  const sample = SAMPLES[sampleIndex];
  const pipelineActive: Record<(typeof PIPELINE)[number]["key"], boolean> = {
    ocr: stage === "scanning",
    analysis: stage === "steps",
    grade: stage === "done",
  };

  return (
    <div className="relative">
      {/* AI glow bleeding out from behind the sheet */}
      <div
        aria-hidden
        className="bg-ai-gradient absolute -inset-6 -z-10 rounded-[2.5rem] opacity-15 blur-3xl"
      />

      {/* Pipeline status chips */}
      <div className="mb-4 flex items-center justify-center gap-2" aria-hidden>
        {PIPELINE.map((p, i) => (
          <div key={p.key} className="flex items-center gap-2">
            <span
              className={cn(
                "flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium transition-colors duration-300",
                pipelineActive[p.key]
                  ? "border-ai-soft bg-ai-soft text-ai"
                  : stage === "done" && p.key !== "grade"
                    ? "border-success/30 bg-success-bg text-success"
                    : "border-border bg-card text-muted-foreground",
              )}
            >
              <p.icon className="size-3.5" strokeWidth={1.75} />
              {p.label}
            </span>
            {i < PIPELINE.length - 1 ? <span className="h-px w-4 bg-border" /> : null}
          </div>
        ))}
      </div>

      {/* The sheet being marked live — visual content is aria-hidden; one
          sr-only live region below narrates the pipeline for screen readers
          instead of announcing every step reveal. */}
      <div className="notebook-sheet relative mx-auto min-h-64 w-full max-w-md overflow-hidden rounded-lg border border-border py-6 pr-6 pl-14 shadow-lg" aria-hidden>
        {stage === "scanning" ? (
          <div className="space-y-3">
            <div className="scan-line" />
            <p className="text-sm text-muted-foreground">{sample.question}</p>
            <p className="flex items-center gap-2 text-xs font-medium text-ai">
              <ScanLine className="animate-pulse size-4" strokeWidth={1.75} />
              Qo&apos;lyozma o&apos;qilyapti…
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            <p className="text-sm leading-8 text-muted-foreground">
              Misol: <span className="font-medium text-foreground">{sample.question}</span>
            </p>
            {sample.steps.slice(0, visibleSteps).map((step, i) => (
              <div key={i} className="animate-rise flex items-start gap-2">
                <InkMark
                  variant={step.correct ? "check" : "cross"}
                  draw
                  className="mt-1 size-4"
                />
                <div>
                  <p
                    className={cn(
                      "text-sm leading-8",
                      step.correct ? "text-foreground" : "text-destructive",
                    )}
                  >
                    {step.text}
                  </p>
                  {step.note ? <p className="text-xs leading-5 text-destructive">{step.note}</p> : null}
                </div>
              </div>
            ))}
          </div>
        )}

        {stage === "done" ? (
          <div className="animate-rise mt-5 flex items-center justify-between gap-4">
            <p className="text-sm text-muted-foreground">{sample.verdict}</p>
            <span className="animate-stamp-in font-heading flex size-14 shrink-0 -rotate-6 items-center justify-center rounded-[50%_48%_52%_49%/48%_52%_49%_51%] border-2 border-destructive text-2xl font-bold text-destructive">
              {sample.grade}
            </span>
          </div>
        ) : null}
      </div>

      {/* Single live region — narrates the stage transition, not each step */}
      <p aria-live="polite" className="sr-only">
        {stage === "scanning"
          ? "Qo'lyozma o'qilyapti"
          : stage === "steps"
            ? "Qadamlar tahlil qilinmoqda"
            : `${sample.verdict} Baho: ${sample.grade} ball`}
      </p>

      {/* Controls — pick a sample or rerun the current one */}
      <div className="mt-4 flex items-center justify-center gap-2">
        {SAMPLES.map((s, i) => (
          <button
            key={i}
            type="button"
            onClick={() => runSample(i)}
            aria-pressed={sampleIndex === i}
            className={cn(
              "rounded-full border px-3 py-1.5 text-xs font-medium transition-colors",
              sampleIndex === i
                ? "border-ai-soft bg-ai-soft text-ai"
                : "border-border bg-card text-muted-foreground hover:border-ai-soft/60 hover:text-foreground",
            )}
          >
            {i + 1}-misol
          </button>
        ))}
        <Button variant="outline" size="sm" onClick={() => runSample(sampleIndex)}>
          <RotateCcw strokeWidth={1.75} />
          Qayta
        </Button>
      </div>
    </div>
  );
}
