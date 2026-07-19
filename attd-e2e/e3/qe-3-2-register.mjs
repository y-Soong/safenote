// QE-3-2 — 과거일(7/16) 사용완료 연차(LV083)에 휴일 등록 "[QE-3-2] 과거일 휴일" + 웹 관찰(Attd_08 판정/월 통계·Attd_09).
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd02, registerHoliday, bodyText } from "./lib-holiday.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await openAttd02(page);
    const r = await registerHoliday(page, 2026, 7, 16, "[QE-3-2] 과거일 휴일");
    console.log("등록 중 dialog:", JSON.stringify(r.dialogs));
    const after = await bodyText(page);
    const idx = after.indexOf("[QE-3-2]");
    console.log("등록 후 캘린더:", idx >= 0 ? after.slice(Math.max(0, idx - 40), idx + 60).replace(/\n/g, " | ") : "미노출!");
    await page.screenshot({ path: shotPath("QE-3-2", "web", "attd02-after-register"), fullPage: true });

    // Attd_08 — A 7월 근태 조회(판정·월 통계)
    await page.goto("http://localhost:8081/safenote/main/Attd_08", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(3000); }
    let t = await bodyText(page);
    // A 행들만 발췌
    const lines = t.split("\n").filter((l) => l.includes("QT사원에이") || l.includes("07.16") || l.includes("2026-07-16"));
    console.log("=== Attd_08 관련 행 ===");
    lines.slice(0, 20).forEach((l) => console.log(l.slice(0, 200)));
    const rows = await page.evaluate(() =>
      [...document.querySelectorAll("tr")].filter((r) => r.innerText.includes("QT사원에이")).map((r) => r.innerText.replace(/\n/g, " | ").slice(0, 220))
    );
    console.log("=== Attd_08 A 행 전체 ===");
    rows.forEach((r) => console.log(r));
    await page.screenshot({ path: shotPath("QE-3-2", "web", "attd08"), fullPage: true });

    // Attd_09 — A 원장 표기
    await page.goto("http://localhost:8081/safenote/main/Attd_09", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    t = await bodyText(page);
    const ai = t.indexOf("QT사원에이");
    console.log("=== Attd_09 A 행 ===");
    console.log(t.slice(ai, ai + 260).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-2", "web", "attd09"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
