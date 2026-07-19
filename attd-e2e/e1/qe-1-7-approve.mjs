// QE-1-7 승인 — 웹 Attd_10 초과근무 상신 탭에서 A의 7/12 OT 승인.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_10", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2000);
    await page.click('button:has-text("초과근무 상신")');
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    const i = text.indexOf("대기 요청");
    console.log("=== OT 탭 ===");
    console.log(text.slice(i, i + 600));
    await page.locator(':text("QT사원에이")').first().click();
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    const j = text.indexOf("요청 정보");
    console.log("=== 상세 ===");
    console.log(text.slice(j, j + 900));
    await page.screenshot({ path: shotPath("QE-1-7", "web", "ot-detail"), fullPage: true });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.locator(':text("요청대로 승인")').first().click();
    await page.waitForTimeout(500);
    await page.click('button:has-text("처리하기")');
    await page.waitForTimeout(1500);
    for (let k = 0; k < 2; k++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1800); }
    }
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 승인 후 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-7", "web", "ot-approved") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
