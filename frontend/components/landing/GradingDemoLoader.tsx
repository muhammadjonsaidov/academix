"use client";

import dynamic from "next/dynamic";

const GradingDemo = dynamic(() => import("./GradingDemo").then((mod) => mod.GradingDemo), {
  ssr: false,
  loading: () => <div className="min-h-[22rem] w-full max-w-md" aria-hidden />,
});

/** Keep the animated demo out of the landing route's initial hydration payload. */
export function GradingDemoLoader() {
  return <GradingDemo />;
}
