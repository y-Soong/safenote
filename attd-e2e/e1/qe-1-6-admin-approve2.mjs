// QE-1-6 승인 실행 — AdminApproval 상세에서 요청대로 승인 → 처리하기.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { clickPopupOk } from "./lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTHR", "QtTest!2026");
    await page.waitForTimeout(2000);
    await page.click('button.app-tabbar__tab:has-text("마이")');
    await page.waitForTimeout(2000);
    await page.locator(':text("관리자 모드")').first().click();
    await page.waitForTimeout(2500);
    await page.locator(':text("승인 관리")').first().click();
    await page.waitForTimeout(2500);
    await page.locator(':text("QT사원에이")').first().click();
    await page.waitForTimeout(2000);
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.locator(':text("요청대로 승인")').first().click();
    await page.waitForTimeout(500);
    await page.click('button:has-text("처리하기")');
    await page.waitForTimeout(1500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 처리 직후 ===");
    console.log(text.split("\n").slice(-15).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "approve-confirm") });
    await clickPopupOk(page, 3);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 최종 ===");
    console.log(text.slice(0, 800));
    await page.screenshot({ path: shotPath("QE-1-6", "app", "approve-done"), fullPage: true });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
