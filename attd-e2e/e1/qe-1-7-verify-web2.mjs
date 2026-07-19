// QE-1-7 웹 면 v2 — Attd_07 A행 7/12 셀 클릭 → 일자상세 OT행.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2000);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(3000);
    // A 행의 셀 구조 덤프
    const cellInfo = await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QT사원에이"));
      if (!row) return null;
      return [...row.children].map((c, idx) => ({ idx, tag: c.tagName, cls: String(c.className).slice(0, 30), txt: (c.innerText || "").replace(/\n/g, "/").slice(0, 30) }));
    });
    console.log("A 행 셀:", JSON.stringify(cellInfo, null, 0));
    if (cellInfo) {
      // 12일 셀(1일=idx1 가정) 클릭
      await page.evaluate(() => {
        const rows = [...document.querySelectorAll("tr")];
        const row = rows.find((r) => r.innerText.includes("QT사원에이"));
        const cell = row.children[12];
        cell.click();
      });
      await page.waitForTimeout(2500);
      const text = await page.evaluate(() => document.body.innerText);
      const i = text.indexOf("2026");
      console.log("=== 셀 클릭 후 ===");
      console.log(text.slice(text.length - 1800));
      await page.screenshot({ path: shotPath("QE-1-7", "web", "attd07-day12-detail"), fullPage: true });
    }
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
