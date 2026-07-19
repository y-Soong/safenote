// QE-3-13 — A 7/15 근태 보정 상신(대기 유지): 19:00~21:00 (E1 잔존 근태와 비겹침).
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { setStepperTime, clickPopupOk } from "../e1/lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERA", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "15");
      if (el) el.click();
    });
    await page.waitForTimeout(2000);
    await page.locator('button:has-text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    await page.locator(':text("근태 보정 요청")').first().click();
    await page.waitForTimeout(2500);
    let text = await page.evaluate(() => document.body.innerText);
    console.log("=== 보정 폼 초기 ===");
    console.log(text.slice(0, 900).replace(/\n/g, " | "));
    const tsf = page.locator("button.tsf-field");
    console.log("tsf 개수:", await tsf.count());
    await setStepperTime(page, tsf.nth(0), 19, 0);
    await setStepperTime(page, tsf.nth(1), 21, 0);
    await page.fill("textarea", "[QE-3-13] 보정 대기 x 휴일 지정 시드");
    await page.waitForTimeout(800);
    await page.screenshot({ path: shotPath("QE-3-13", "app", "corr-form-filled"), fullPage: true });
    const submit = page.locator('button:has-text("요청하기")').last();
    console.log("요청하기 disabled=", await submit.isDisabled().catch(() => "?"));
    await submit.click();
    await page.waitForTimeout(2500);
    text = await page.evaluate(() => document.body.innerText);
    console.log("=== 제출 직후 ===");
    console.log(text.split("\n").slice(-10).join(" | "));
    await clickPopupOk(page);
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
