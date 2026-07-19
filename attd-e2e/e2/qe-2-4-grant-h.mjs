// QE-2-4 시드 — 웹 Attd_09에서 QT사원H에게 포상 연차 1일 수동 부여.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_09", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2500);
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(2500); }
    // H 행의 체크박스 선택
    await page.evaluate(() => {
      const rows = [...document.querySelectorAll("tr")];
      const row = rows.find((r) => r.innerText.includes("QT사원H"));
      if (!row) throw new Error("H 행 미발견");
      const cb = row.querySelector('input[type="checkbox"]');
      if (!cb) throw new Error("체크박스 미발견");
      cb.click();
    });
    await page.waitForTimeout(800);
    await page.click('button:has-text("일괄 수동 부여")');
    await page.waitForTimeout(2000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 수동 부여 팝업 ===");
    console.log(text.slice(text.indexOf("수동 부여")).slice(0, 1500));
    await page.screenshot({ path: shotPath("QE-2-4", "web", "grant-popup"), fullPage: true });
    // 팝업 내 폼 요소 실측
    const selects = await page.locator(".modal select, .popup select, select:visible").allInnerTexts();
    console.log("select 옵션:", JSON.stringify(selects).slice(0, 800));
    const inputs = await page.locator('input:visible').evaluateAll((els) => els.map((e) => ({ type: e.type, ph: e.placeholder, name: e.name, cls: e.className.slice(0, 40) })));
    console.log("input 목록:", JSON.stringify(inputs).slice(0, 1200));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
