// Dates the Uzbek way: "19-noyabr, 16:00". Written by hand instead of Intl("uz-UZ"),
// whose output differs between Node and browsers (and would break hydration).

const MONTHS = [
  "yanvar",
  "fevral",
  "mart",
  "aprel",
  "may",
  "iyun",
  "iyul",
  "avgust",
  "sentabr",
  "oktabr",
  "noyabr",
  "dekabr",
];

type DateInput = string | number | Date | null | undefined;

function toDate(value: DateInput): Date | null {
  if (value === null || value === undefined || value === "") return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function pad(value: number): string {
  return value.toString().padStart(2, "0");
}

/** "19-noyabr" — the year is added only when it isn't the current one. */
export function formatDate(value: DateInput, now: Date = new Date()): string {
  const date = toDate(value);
  if (!date) return "—";
  const day = `${date.getDate()}-${MONTHS[date.getMonth()]}`;
  return date.getFullYear() === now.getFullYear() ? day : `${date.getFullYear()}-yil ${day}`;
}

/** "19-noyabr, 16:00" */
export function formatDateTime(value: DateInput, now: Date = new Date()): string {
  const date = toDate(value);
  if (!date) return "—";
  return `${formatDate(date, now)}, ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/** "19.11" — compact, for chart axes. */
export function formatShortDate(value: DateInput): string {
  const date = toDate(value);
  if (!date) return "—";
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}`;
}

/**
 * How far away a deadline is, in words a student reads at a glance:
 * "muddati o'tgan", "bugun 18:00 gacha", "ertaga 18:00 gacha", "3 kun qoldi".
 */
export function deadlineLabel(value: DateInput, now: Date = new Date()): string {
  const date = toDate(value);
  if (!date) return "—";
  if (date.getTime() < now.getTime()) return "muddati o'tgan";
  const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  const days = Math.round((startOfDay(date) - startOfDay(now)) / 86_400_000);
  const time = `${pad(date.getHours())}:${pad(date.getMinutes())}`;
  if (days === 0) return `bugun ${time} gacha`;
  if (days === 1) return `ertaga ${time} gacha`;
  return `${days} kun qoldi`;
}
