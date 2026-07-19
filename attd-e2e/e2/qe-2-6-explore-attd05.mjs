// QE-2-6 탐색 — 웹 Attd_05 근무계획 그리드 구조/연차 배정 수단 실측.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_05", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== Attd_05 초기 ===");
    console.log(text.slice(0, 2500));
    await page.screenshot({ path: shotPath("QE-2-6", "web", "attd05-initial"), fullPage: true });
    // 조회
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(3000); }
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 조회 후 ===");
    console.log(text.slice(0, 3000));
    await page.screenshot({ path: shotPath("QE-2-6", "web", "attd05-loaded"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
