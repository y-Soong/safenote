// Attd_05 특정 사용자 행 8월 그리드 채집 — node e3/web-attd05-row.mjs <UID> <caseId> <label> [nextMonth=1]
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , uid, caseId, label, nextMonth = "1"] = process.argv;

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    if (nextMonth === "1") {
      // 조회월 = flatpickr(calendar-input) → 2026-08 세팅 후 조회
      await page.evaluate(() => {
        const el = document.querySelector("input.calendar-input");
        if (el && el._flatpickr) el._flatpickr.setDate("2026-08-01", true);
      });
      await page.waitForTimeout(800);
      await page.click('button:has-text("조회")');
      await page.waitForTimeout(3000);
    }
    const monthTxt = await page.evaluate(() => document.body.innerText.match(/\d{4}년\s*\d{1,2}월|\d{4}[.-]\d{2}/)?.[0] ?? "(미발견)");
    console.log("그리드 월:", monthTxt);
    const row = await page.evaluate((u) => {
      const rows = [...document.querySelectorAll("tr")];
      const r = rows.find((x) => x.innerText.includes(u));
      if (!r) return null;
      const tds = [...r.querySelectorAll("td")];
      return tds.map((t, i) => `${i}:${t.innerText.trim().replace(/\n/g, "/")}[${t.className.replace(/td-day\s?/, "")}]`).join(" | ");
    }, uid);
    console.log(`=== ${uid} 행 ===`);
    console.log(row ?? "행 미발견");
    await page.screenshot({ path: shotPath(caseId, "web", `attd05-${label}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
