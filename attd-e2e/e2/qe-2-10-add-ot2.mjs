// QE-2-10 ① — OT 구간 09:00~10:00 입력 → 초과근무 저장.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const q = page.locator('button:has-text("조회")').first();
    await q.click(); await page.waitForTimeout(3000);
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
    await page.waitForTimeout(1200);
    const ot = page.locator("input.ot-time:visible");
    await ot.nth(0).click();
    await ot.nth(0).pressSequentially("0900", { delay: 60 });
    await ot.nth(0).press("Tab");
    await page.waitForTimeout(300);
    await ot.nth(1).click();
    await ot.nth(1).pressSequentially("1000", { delay: 60 });
    await ot.nth(1).press("Tab");
    await page.waitForTimeout(400);
    console.log("ot values:", await ot.nth(0).inputValue(), await ot.nth(1).inputValue());
    const ta = page.locator("textarea:visible").last();
    if (await ta.count()) await ta.fill("[QE-2-10] 웹 직접등록 OT");
    await page.screenshot({ path: shotPath("QE-2-10", "web", "ot-filled"), fullPage: true });
    await page.locator('button:has-text("초과근무 저장")').click();
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 저장 응답 ===");
    console.log(text.split("\n").slice(-14).join(" | "));
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    }
    text = await page.evaluate(() => document.body.innerText);
    const oi = text.indexOf("초과근무");
    console.log("=== OT 영역(저장 후) ===");
    console.log(text.slice(oi, oi + 700));
    await page.screenshot({ path: shotPath("QE-2-10", "web", "ot-saved"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
