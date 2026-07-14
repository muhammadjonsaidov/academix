import { render, screen } from "@testing-library/react";
import { Button } from "@/components/ui/button";

test("renders button text", () => {
  render(<Button>Kirish</Button>);
  expect(screen.getByRole("button", { name: "Kirish" })).toBeInTheDocument();
});
