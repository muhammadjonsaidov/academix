import { describe, expect, it } from "vitest";
import { deadlineLabel, formatDate, formatDateTime, formatShortDate } from "../format";

const now = new Date(2026, 10, 19, 10, 0);

describe("Uzbek date formatting", () => {
  it("writes the day and month in Uzbek, adding the year only when it differs", () => {
    expect(formatDate(new Date(2026, 10, 19), now)).toBe("19-noyabr");
    expect(formatDate(new Date(2025, 0, 3), now)).toBe("2025-yil 3-yanvar");
  });

  it("adds a 24-hour time", () => {
    expect(formatDateTime(new Date(2026, 8, 1, 8, 5), now)).toBe("1-sentabr, 08:05");
  });

  it("returns a dash for missing or invalid input", () => {
    expect(formatDate(null)).toBe("—");
    expect(formatDateTime("not a date")).toBe("—");
  });

  it("formats compact chart dates", () => {
    expect(formatShortDate(new Date(2026, 1, 7))).toBe("07.02");
  });

  it("describes deadlines in words", () => {
    expect(deadlineLabel(new Date(2026, 10, 19, 9, 0), now)).toBe("muddati o'tgan");
    expect(deadlineLabel(new Date(2026, 10, 19, 18, 0), now)).toBe("bugun 18:00 gacha");
    expect(deadlineLabel(new Date(2026, 10, 20, 18, 0), now)).toBe("ertaga 18:00 gacha");
    expect(deadlineLabel(new Date(2026, 10, 23, 9, 0), now)).toBe("4 kun qoldi");
  });
});
