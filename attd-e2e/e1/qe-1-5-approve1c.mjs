// QE-1-5 1건째 승인 실행 — 요청대로 승인 라디오 → 처리하기.
import { webLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";

const main = async () => {
  try {
    const { page } = await webLogin("QTHR", "QtTest!2026");
    await page.goto("http://localhost:8081/safenote/main/Attd_10", { waitUntil: "networkidle", timeout: 30000 });
    await page.waitForTimeout(2000);
    await page.click('button:has-text("근태 보정")');
    await page.waitForTimeout(2500);
    await page.locator(':text("QT자기승인씨")').first().click();
    await page.waitForTimeout(2000);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.locator('label:has-text("요청대로 승인"), :text("요청대로 승인")').first().click();
    await page.waitForTimeout(500);
    await page.click('button:has-text("처리하기")');
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 처리 직후 ===");
    console.log(text.split("\n").slice(-25).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-5", "web", "approve1-result"), fullPage: true });
    // 팝업 확인 처리
    const ok = page.locator("button:has-text('확인')").first();
    if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1500); }
    text = await page.evaluate(() => document.body.innerText);
    const i = text.indexOf("대기 요청");
    console.log("=== 최종 목록 ===");
    console.log(text.slice(i, i + 400));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
