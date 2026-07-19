// QE-1-5 2건째 — QTUSERC 7/15 02:00~04:00 겹침 보정 상신(승인 시 차단 기대).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { setStepperTime, clickPopupOk, openReqForm } from "./lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERC", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });

    await openReqForm(page, "수\\s*15", "근태 보정 요청");
    console.log("URL:", page.url());
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 폼 초기(7/15) ===");
    console.log(text.slice(0, 700));

    await setStepperTime(page, page.locator("button.tsf-field").nth(0), 2, 0);
    await setStepperTime(page, page.locator("button.tsf-field").nth(1), 4, 0);
    await page.fill("textarea", "[QE-1-5] 겹침 보정(0200~0400) — 승인차단 기대");
    await page.waitForTimeout(500);
    await page.screenshot({ path: shotPath("QE-1-5", "app", "form-filled-2nd"), fullPage: true });
    await page.click('button:has-text("요청하기")');
    await page.waitForTimeout(2000);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-15).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-5", "app", "submit2-response") });
    await clickPopupOk(page);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
