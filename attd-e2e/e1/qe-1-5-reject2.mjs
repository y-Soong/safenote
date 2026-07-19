// QE-1-5 정리 — 겹침 보정(0200~0400) 반려 처리.
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
    await page.locator(':text("반려")').first().click();
    await page.waitForTimeout(500);
    // 반려 사유 입력란 있으면 채움
    const reasonBox = page.locator("textarea:visible, input[placeholder*='사유']:visible").last();
    if (await reasonBox.count()) await reasonBox.fill("[QE-1-5] 테스트 요청 반려(겹침 차단 확인 완료)").catch(() => {});
    await page.click('button:has-text("처리하기")');
    await page.waitForTimeout(1500);
    // 확인 모달 처리(반려 처리하시겠습니까?)
    for (let i = 0; i < 2; i++) {
      const ok = page.locator("button:has-text('확인')").first();
      if (await ok.count()) { await ok.click().catch(() => {}); await page.waitForTimeout(1800); }
    }
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 반려 처리 후 ===");
    console.log(text.split("\n").slice(-15).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-5", "web", "reject2-result") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
