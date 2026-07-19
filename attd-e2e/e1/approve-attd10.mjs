// 웹 Attd_10 승인 공용 — node e1/approve-attd10.mjs <탭텍스트> <요청자명> [caseId]
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const [, , tab = "근태 보정", who = "QT사원에이", caseId = "QE-x"] = process.argv;
const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_10", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2000);
    await page.click(`button:has-text("${tab}")`);
    await page.waitForTimeout(2500);
    await page.locator(`:text("${who}")`).first().click();
    await page.waitForTimeout(2000);
    await page.locator(':text("요청대로 승인")').first().click();
    await page.waitForTimeout(500);
    await page.click('button:has-text("처리하기")');
    await page.waitForTimeout(1500);
    for (let k = 0; k < 2; k++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1800); }
    }
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 승인 후 ===");
    console.log(text.split("\n").slice(-10).join(" | "));
    await page.screenshot({ path: shotPath(caseId, "web", "attd10-approved") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
