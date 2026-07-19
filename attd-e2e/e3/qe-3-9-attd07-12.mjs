// QE-3-9 — 웹 Attd_07 7/12(휴일 지정됨) 일자상세: 승인 OT(2026071700044) 표기 관찰.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(3000); }
    // A 행의 12일 셀 더블클릭
    const cell = await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QT사원에이"));
      if (!row) return "A 행 미발견";
      const tds = [...row.querySelectorAll("td.m-day-cell")];
      const td = tds[11]; // 12일
      if (!td) return `m-day-cell ${tds.length}개`;
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim().replace(/\n/g, "/"), cls: td.className };
    });
    console.log("12일 셀:", JSON.stringify(cell));
    if (typeof cell === "string") throw new Error(cell);
    await page.mouse.dblclick(cell.x, cell.y);
    await page.waitForTimeout(2500);
    const popup = await page.evaluate(() => document.body.innerText);
    const pi = popup.indexOf("2026");
    console.log("=== 일자상세 팝업 ===");
    // 팝업 텍스트 후반부(팝업이 마지막에 렌더)
    console.log(popup.slice(-1300).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-3-9", "web", "attd07-day12-detail"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
