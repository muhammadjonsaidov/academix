"use client";

import dynamic from "next/dynamic";

const NotebookDemo = dynamic(() => import("./NotebookDemo").then((mod) => mod.NotebookDemo), {
  ssr: false,
  loading: () => <div className="min-h-[26rem] w-full max-w-lg" aria-hidden />,
});

/** Keeps the animated notebook out of the landing route's initial hydration payload. */
export function NotebookDemoLoader() {
  return <NotebookDemo />;
}
