"use client";

import { useEffect, useState } from "react";
import { GradeMark } from "@/components/shared/GradeMark";
import { cn } from "@/lib/utils";

/**
 * The landing page's one orchestrated moment: a squared notebook page with a student's work in
 * blue ink; the AI's notes appear in pencil, then the teacher's red circle is drawn. Three
 * subjects show the three checking modes. Reduced motion shows the finished page at once.
 */

interface Line {
  text: string;
  /** The AI's pencil note in the margin. */
  note?: string;
  /** A wrong fragment: struck through in red, correction written above. */
  wrong?: { fragment: string; correction: string };
}

interface Sample {
  id: string;
  subject: string;
  mode: string;
  task: string;
  lines: Line[];
  aiScore: number;
  grade: number;
}

const SAMPLES: Sample[] = [
  {
    id: "math",
    subject: "Matematika",
    mode: "Masala yechish",
    task: "Tenglamani yeching: 5x + 3 = 18",
    lines: [
      { text: "5x + 3 = 18", note: "shart to'g'ri" },
      { text: "5x = 18 − 3", note: "to'g'ri" },
      { text: "5x = 15", note: "to'g'ri" },
      { text: "x = 3", note: "javob to'g'ri" },
    ],
    aiScore: 96,
    grade: 5,
  },
  {
    id: "uz",
    subject: "Ona tili",
    mode: "Til va yozuv",
    task: "Diktant: «Kuz»",
    lines: [
      { text: "Kuz keldi. Daraxtlar sarg'aydi." },
      { text: "Biz kecha maktabda bayram o'tkazdik.", wrong: { fragment: "maktapda", correction: "maktabda" } },
      { text: "Bolalar she'r o'qishdi", note: "nuqta yo'q" },
    ],
    aiScore: 78,
    grade: 4,
  },
  {
    id: "history",
    subject: "Tarix",
    mode: "Yozma javob",
    task: "Amir Temur davlatining poytaxti qaysi shahar edi?",
    lines: [
      { text: "Amir Temur 1370-yilda hokimiyatga keldi.", note: "fakt to'g'ri" },
      { text: "Davlat poytaxti Samarqand edi.", note: "to'g'ri" },
      { text: "U ilm-fanga homiylik qildi.", note: "dalil kerak" },
    ],
    aiScore: 84,
    grade: 4,
  },
];

type Stage = 0 | 1 | 2 | 3; // 0 page, 1 pencil notes, 2 pencil score, 3 red grade

export function NotebookDemo() {
  const [index, setIndex] = useState(0);
  const [stage, setStage] = useState<Stage>(0);
  const sample = SAMPLES[index];

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced) {
      const id = window.setTimeout(() => setStage(3), 0);
      return () => window.clearTimeout(id);
    }
    const timers = [
      window.setTimeout(() => setStage(0), 0),
      window.setTimeout(() => setStage(1), 700),
      window.setTimeout(() => setStage(2), 1700),
      window.setTimeout(() => setStage(3), 2500),
    ];
    return () => timers.forEach((id) => window.clearTimeout(id));
  }, [index]);

  return (
    <figure className="w-full max-w-lg">
      <div role="tablist" aria-label="Fan namunasi" className="mb-3 flex gap-1">
        {SAMPLES.map((item, i) => (
          <button
            key={item.id}
            type="button"
            role="tab"
            aria-selected={i === index}
            onClick={() => setIndex(i)}
            className={cn(
              "rounded-md px-3 py-1.5 text-sm transition-colors focus-visible:outline-2 focus-visible:outline-ring",
              i === index ? "bg-ink text-primary-foreground" : "text-muted-foreground hover:bg-accent hover:text-foreground",
            )}
          >
            {item.subject}
          </button>
        ))}
      </div>

      <div className="notebook-sheet relative rounded-[10px] border border-border py-5 pr-5 pl-14 shadow-[0_1px_2px_rgb(22_40_74/0.06),0_12px_32px_-12px_rgb(22_40_74/0.18)]">
        <p className="mb-3 text-sm text-muted-foreground">{sample.task}</p>
        <ol className="space-y-1.5">
          {sample.lines.map((line, i) => (
            <li key={`${sample.id}-${i}`} className="grid grid-cols-[minmax(0,1fr)_auto] items-baseline gap-3">
              <span className="font-hand text-[1.45rem] leading-8 text-[#1f4fa8] dark:text-[#8db4ff]">
                {line.wrong ? <WrongFragment line={line} visible={stage >= 1} /> : line.text}
              </span>
              <span
                className={cn(
                  "text-xs text-pencil transition-opacity duration-500",
                  stage >= 1 ? "opacity-100" : "opacity-0",
                )}
                style={{ transitionDelay: stage >= 1 ? `${i * 120}ms` : "0ms" }}
              >
                {line.note ?? ""}
              </span>
            </li>
          ))}
        </ol>

        <div className="mt-5 flex items-end justify-between gap-4">
          <p
            className={cn(
              "border-b border-dashed border-pencil/60 text-sm text-pencil transition-opacity duration-500",
              stage >= 2 ? "opacity-100" : "opacity-0",
            )}
          >
            AI taklifi: <span className="font-data">{sample.aiScore}%</span>
          </p>
          <div className={cn("transition-opacity", stage >= 3 ? "opacity-100" : "opacity-0")}>
            {stage >= 3 ? <GradeMark key={sample.id} grade={sample.grade} size="lg" draw /> : <span className="block size-20" />}
          </div>
        </div>
      </div>

      <figcaption className="mt-3 text-sm text-muted-foreground">
        {sample.mode}: AI qalamda yozadi, yakuniy bahoni o&apos;qituvchi qizil ruchkada qo&apos;yadi.
      </figcaption>
    </figure>
  );
}

function WrongFragment({ line, visible }: { line: Line; visible: boolean }) {
  const wrong = line.wrong!;
  const [before, after] = line.text.split(wrong.correction);
  return (
    <>
      {before}
      <span className="relative inline-block">
        <span className={cn(visible && "text-pen-red line-through decoration-2")}>{wrong.fragment}</span>
        <span
          className={cn(
            "absolute -top-4 left-0 text-base whitespace-nowrap text-pen-red transition-opacity duration-500",
            visible ? "opacity-100" : "opacity-0",
          )}
        >
          {wrong.correction}
        </span>
      </span>
      {after}
    </>
  );
}
