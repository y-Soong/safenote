// QE-1-5 1건째 — QTUSERC 7/14 22:00 ~ 7/15 04:30 오버나이트 보정 상신.
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { setStepperTime, setStepperDate, clickPopupOk, openReqForm } from "./lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERC", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });

    await openReqForm(page, "화\\s*14", "근태 보정 요청");
    console.log("URL:", page.url());

    // 출근 22:00
    await setStepperTime(page, page.locator("button.tsf-field").nth(0), 22, 0);
    // 퇴근일 7/15
    await setStepperDate(page, page.locator("button.dsf-field").nth(1), 2026, 7, 15);
    // 퇴근 04:30
    await setStepperTime(page, page.locator("button.tsf-field").nth(1), 4, 30);
    // 사유
    await page.fill("textarea", "[QE-1-5] 오버나이트 근태 보정(2200~익일0430)");
    await page.waitForTimeout(500);
    await page.screenshot({ path: shotPath("QE-1-5", "app", "form-filled-1st"), fullPage: true });

    const submit = page.locator('button:has-text("요청하기")');
    console.log("요청하기 disabled=", await submit.isDisabled());
    await submit.click();
    await page.waitForTimeout(2000);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-20).join(" | "));
    await page.screenshot({ path: shotPath("QE-1-5", "app", "submit1-response") });
    await clickPopupOk(page);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 처리 후 ===");
    console.log(text.slice(0, 500));
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
