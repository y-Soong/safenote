// QE-2-9 — Attd_05: QTUSERG 오늘(7/17) 셀 법정 휴가 적용 → 저장 (당일 배정 가능 여부 관찰).
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const day = Number(process.argv[2] || "17");
const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    const cellInfo = await page.evaluate((dd) => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QTUSERG"));
      const tds = [...row.querySelectorAll("td")];
      const td = tds.slice(-31)[dd - 1];
      td.scrollIntoView();
      const r = td.getBoundingClientRect();
      return { x: r.x + r.width / 2, y: r.y + r.height / 2, text: td.innerText.trim() };
    }, day);
    console.log(`${day}일 셀:`, JSON.stringify(cellInfo));
    await page.mouse.click(cellInfo.x, cellInfo.y);
    await page.waitForTimeout(800);
    await page.locator('button:has-text("적용")').nth(1).click();
    await page.waitForTimeout(1500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 적용 직후 알림/셀 ===");
    console.log(text.split("\n").slice(-10).join(" | "));
    const gi = text.indexOf("QTUSERG");
    console.log("G행:", text.slice(gi, gi + 380).replace(/\n/g, " "));
    await page.screenshot({ path: shotPath("QE-2-9", "web", `apply-day${day}`), fullPage: true });
    await page.click('button:has-text("저장")');
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 저장 응답 ===");
    console.log(text.split("\n").slice(-10).join(" | "));
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    }
    text = await page.evaluate(() => document.body.innerText);
    const gi2 = text.indexOf("QTUSERG");
    console.log("=== 저장 후 G행 ===");
    console.log(text.slice(gi2, gi2 + 380).replace(/\n/g, " "));
    await page.screenshot({ path: shotPath("QE-2-9", "web", `saved-day${day}`), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
