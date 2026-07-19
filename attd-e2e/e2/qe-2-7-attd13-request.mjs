// QE-2-7 — 웹 Attd_13(연차 변경 동의 관리): QE-2-6 건(D 7/30) DELETE 요청 생성.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.goto("http://localhost:8081/safenote/main/Attd_13", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(3000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== Attd_13 초기 ===");
    console.log(text.slice(0, 2500));
    await page.screenshot({ path: shotPath("QE-2-7", "web", "attd13-initial"), fullPage: true });
    const q = page.locator('button:has-text("조회")').first();
    if (await q.count()) { await q.click(); await page.waitForTimeout(2500); }
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 조회 후 ===");
    console.log(text.slice(text.indexOf("연차 변경")).slice(0, 2500));
    await page.screenshot({ path: shotPath("QE-2-7", "web", "attd13-loaded"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
