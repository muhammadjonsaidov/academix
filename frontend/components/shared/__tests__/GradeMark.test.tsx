import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { GradeMark, PencilScore } from "../GradeMark";

describe("GradeMark", () => {
  it("announces the confirmed grade to screen readers", () => {
    render(<GradeMark grade={4} />);
    expect(screen.getByRole("img", { name: "Baho: 4" })).toBeInTheDocument();
  });
});

describe("PencilScore", () => {
  it("rounds the AI suggestion and shows the suggested grade", () => {
    render(<PencilScore percent={88.4} grade={5} />);
    expect(screen.getByText("88%")).toBeInTheDocument();
    expect(screen.getByText("(5)")).toBeInTheDocument();
    expect(screen.getByTitle(/yakuniy bahoni o'qituvchi qo'yadi/)).toBeInTheDocument();
  });
});
