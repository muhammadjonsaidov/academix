// One-off audit: login as each role, collect console errors, horizontal overflow,
// and presence of key dashboard elements. Screenshots saved to project root.
import { chromium } from "playwright";

const BASE = "http://localhost:3000";
const PASSWORD = "Test1234!";
const accounts = [
  ["ADMIN", "+998901234567"],
  ["TEACHER", "+998911112233"],
  ["STUDENT", "+998933334455"],
  ["PARENT", "+998977001122"],
  ["PSYCHOLOGIST", "+998955501234"],
];

const browser = await chromium.launch({ executablePath: "/usr/bin/chromium", args: ["--no-sandbox"] });
const results = [];
for (const [role, phone] of accounts) {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = [];
  page.on("console", (m) => {
    if (m.type() === "error") errors.push(m.text().slice(0, 200));
  });
  page.on("pageerror", (e) => errors.push(String(e).slice(0, 200)));

  await page.goto(`${BASE}/login`, { waitUntil: "domcontentloaded" });
  await page.fill("#phone", phone);
  await page.fill("#password", PASSWORD);
  await page.click('button[type="submit"]');
  try {
    await page.waitForURL("**/dashboard/**", { timeout: 15000 });
  } catch {
    errors.push("NAVIGATION_TIMEOUT");
  }
  await page.waitForTimeout(3500);

  const overflow = await page.evaluate(() => {
    const d = document.documentElement;
    return { overflow: d.scrollWidth > d.clientWidth + 1, scrollW: d.scrollWidth, clientW: d.clientWidth };
  });
  const counts = await page.evaluate(() => ({
    hero: document.querySelectorAll("section.rounded-xl").length,
    progressbars: document.querySelectorAll('[role="progressbar"]').length,
    statTiles: document.querySelectorAll('[data-slot="card"]').length,
    bodyText: document.body.innerText.length,
  }));

  await page.screenshot({ path: `shots-${role.toLowerCase()}.png`, fullPage: true });
  results.push({ role, url: page.url(), errors: [...new Set(errors)].slice(0, 5), overflow, counts });
  await page.close();
}
await browser.close();
console.log(JSON.stringify(results, null, 2));
