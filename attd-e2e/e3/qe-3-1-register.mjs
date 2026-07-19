// QE-3-1 — A 7/27 확정 연차일(LV2026071700072)에 휴일 등록 "[QE-3-1] 대체공휴일" + 웹 관찰.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { openAttd02, registerHoliday, bodyText } from "./lib-holiday.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await openAttd02(page);
    const before = await bodyText(page);
    console.log("등록 전 7월 캘린더 27일 주변:", before.slice(before.indexOf("26"), before.indexOf("26") + 120).replace(/\n/g, " | "));
    const r = await registerHoliday(page, 2026, 7, 27, "[QE-3-1] 대체공휴일");
    console.log("등록 중 dialog:", JSON.stringify(r.dialogs));
    console.log("일자 프리필:", r.dateVal);
    // 등록 직후 캘린더/목록 반영
    const after = await bodyText(page);
    const idx = after.indexOf("[QE-3-1]");
    console.log("등록 후 반영:", idx >= 0 ? after.slice(Math.max(0, idx - 60), idx + 80).replace(/\n/g, " | ") : "미노출!");
    await page.screenshot({ path: shotPath("QE-3-1", "web", "attd02-after-register"), fullPage: true });

    // 웹 Attd_09 — A 행 사용예정/잔여 표기
    await page.goto("http://localhost:8081/safenote/main/Attd_09", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    let t = await bodyText(page);
    const ai = t.indexOf("QT사원에이");
    console.log("=== Attd_09 A 행 ===");
    console.log(ai >= 0 ? t.slice(ai, ai + 300).replace(/\n/g, " | ") : t.slice(0, 500));
    await page.screenshot({ path: shotPath("QE-3-1", "web", "attd09"), fullPage: true });

    // 웹 Attd_05 7월 그리드 — A 27일 셀
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const cell = await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QTUSERA"));
      if (!row) return "A 행 미발견";
      const tds = [...row.querySelectorAll("td")].slice(-31);
      return { d26: tds[25].innerText.trim(), d27: tds[26].innerText.trim(), d27cls: tds[26].className, d28: tds[27].innerText.trim() };
    });
    console.log("=== Attd_05 A 26/27/28일 셀 ===", JSON.stringify(cell));
    await page.screenshot({ path: shotPath("QE-3-1", "web", "attd05"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
