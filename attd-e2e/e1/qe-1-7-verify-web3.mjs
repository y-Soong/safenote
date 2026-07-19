// QE-1-7 웹 면 v3 — Attd_07 A행 7/12 셀 내부 요소 클릭 → 일자상세 팝업 OT행.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_07", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2000);
    await page.click('button:has-text("조회")');
    await page.waitForTimeout(3000);
    // A 행 12일 셀 내부 클릭 (Playwright 실클릭 — 이벤트 위임 대응)
    const cell = page.locator("tr", { hasText: "QTUSERA" }).locator("td").nth(12);
    await cell.dblclick();
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    // 팝업 감지 — 마지막 부분 덤프
    console.log("=== 클릭 후 말미 ===");
    console.log(text.slice(-1500));
    await page.screenshot({ path: shotPath("QE-1-7", "web", "attd07-day12-detail"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
