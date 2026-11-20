import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TutorBubble } from "../ChatMessage";

const ANSWER = `# Foizlarni o'rganish

Avval bilib olaylik:

- "1%" degani nima?
- 50% va 100% nimani anglatadi?

## 1-qadam: Foiz — bu "yuzdan biri"

"Foiz" so'zi lotincha *per cent*. Har qanday foizni **100 ga bo'lish** mumkin: $25\\% = 25/100$.

<script>alert(1)</script>`;

describe("TutorBubble", () => {
  it("renders the tutor's markdown instead of showing the raw symbols", () => {
    const { container } = render(<TutorBubble text={ANSWER} />);

    expect(screen.getByText("Foizlarni o'rganish")).toBeInTheDocument();
    expect(screen.getAllByRole("listitem")).toHaveLength(2);
    expect(screen.getByText("100 ga bo'lish").tagName).toBe("STRONG");
    expect(screen.getByText("per cent").tagName).toBe("EM");
    expect(screen.getByText("25\\% = 25/100").tagName).toBe("CODE");
    expect(container.textContent).not.toMatch(/(^|\n)#|\*\*/);
    expect(container.querySelector("script")).toBeNull();
  });
});
