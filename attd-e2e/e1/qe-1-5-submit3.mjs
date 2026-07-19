// QE-1-5 오탐 회귀 — QTUSERC 7/15 09:00~18:00 정상 보정 상신(승인 성공 기대).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { setStepperTime, clickPopupOk, openReqForm } from "./lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERC", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await openReqForm(page, "수\\s*15", "근태 보정 요청");
    await setStepperTime(page, page.locator("button.tsf-field").nth(0), 9, 0);
    await setStepperTime(page, page.locator("button.tsf-field").nth(1), 18, 0);
    await page.fill("textarea", "[QE-1-5] 오탐회귀 정상보정(0900~1800)");
    await page.waitForTimeout(500);
    await page.click('button:has-text("요청하기")');
    await page.waitForTimeout(2000);
    const text = await page.evaluate(() => document.body.innerText);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-12).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-5", "app", "submit3-response") });
    await clickPopupOk(page);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
