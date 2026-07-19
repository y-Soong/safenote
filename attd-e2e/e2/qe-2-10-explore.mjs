// QE-2-10 탐색 — Attd_07 일자상세(더블클릭) 구조 + 초과근무 편집 UI 실측.
// 사용: node e2/qe-2-10-explore.mjs <이름> <day>
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , who = "QT자기승인씨", dayStr = "15"] = process.argv;
const day = Number(dayStr);
const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(3000); }
    const cellInfo = await page.evaluate(({ nm, dd }) => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes(nm));
      if (!row) return "행 미발견";
      const tds = [...row.querySelectorAll("td.m-day-cell")];
      if (tds.length === 0) return "m-day-cell 없음: td수=" + row.querySelectorAll("td").length;
      const td = tds[dd - 1];
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim(), n: tds.length };
    }, { nm: who, dd: day });
    console.log("셀:", JSON.stringify(cellInfo));
    if (typeof cellInfo === "string") throw new Error(cellInfo);
    await page.mouse.dblclick(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(2500);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 일자상세 ===");
    console.log(text.slice(text.length - 2500));
    await page.screenshot({ path: shotPath("QE-2-10", "web", `day-detail-${who}-${day}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
