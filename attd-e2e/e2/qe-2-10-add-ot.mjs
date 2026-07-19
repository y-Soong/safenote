// QE-2-10 ① — C 7/15 일자상세에서 초과근무 구간 직접 등록(09:00~10:00).
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
    const cellInfo = await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QT자기승인씨"));
      const td = [...row.querySelectorAll("td.m-day-cell")][14];
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2 };
    });
    await page.mouse.dblclick(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(2500);
    await page.locator('button:has-text("1구간 초과근무 추가")').click();
    await page.waitForTimeout(1500);
    // 추가된 OT 행의 입력 실측
    const inputs = await page.locator("input:visible").evaluateAll((els) => els.map((e, i) => ({ i, type: e.type, val: e.value, ph: e.placeholder, cls: e.className.slice(0, 50) })));
    console.log("inputs:", JSON.stringify(inputs, null, 1).slice(0, 2000));
    let text = await page.evaluate(() => document.body.innerText);
    const oi = text.indexOf("초과근무");
    console.log("=== OT 편집 영역 ===");
    console.log(text.slice(oi, oi + 800));
    await page.screenshot({ path: shotPath("QE-2-10", "web", "ot-add-row"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
