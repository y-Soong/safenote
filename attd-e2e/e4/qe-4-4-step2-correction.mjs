// QE-4-4 step2 — H 7/16(QE9H 배정일) 근태 보정 상신 10:00~15:00
import { appLogin, closeAll } from "../lib/browser.mjs";
import { shotPath } from "../lib/record.mjs";
import { setStepperTime, clickPopupOk } from "../e1/lib-form.mjs";

const main = async () => {
  try {
    const { page } = await appLogin("QTUSERH", "QtTest!2026");
    await page.waitForFunction(() => !document.body.innerText.includes("불러오는 중"), { timeout: 20000 });
    page.on("dialog", async (d) => { console.log("DIALOG:", d.message()); await d.accept(); });
    await page.click('button.app-tabbar__tab:has-text("근태")');
    await page.waitForTimeout(2000);
    await page.click('.attd-seg__item:has-text("이번달")');
    await page.waitForTimeout(2500);
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "16" && c.className.includes("cal"));
      if (el) el.click();
    });
    await page.waitForTimeout(2000);
    const dayDetail = await page.evaluate(() => document.body.innerText);
    console.log("7/16 상세(보정 전):", dayDetail.slice(-500).replace(/\n/g, " | "));
    await page.screenshot({ path: shotPath("QE-4-4", "app", "h-0716-before"), fullPage: true });
    await page.locator('button:has-text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    await page.locator(':text("근태 보정 요청")').first().click();
    await page.waitForTimeout(2500);
    const tsf = page.locator("button.tsf-field");
    await setStepperTime(page, tsf.nth(0), 10, 0);
    await setStepperTime(page, tsf.nth(1), 15, 0);
    await page.fill("textarea", "[QE-4-4] QE9H 과거 근태 시드 보정");
    await page.waitForTimeout(800);
    const submit = page.locator('button:has-text("요청하기")').last();
    console.log("요청하기 disabled=", await submit.isDisabled().catch(() => "?"));
    await submit.click();
    await page.waitForTimeout(2500);
    console.log("제출 직후:", (await page.evaluate(() => document.body.innerText)).split("\n").slice(-10).join(" | "));
    await clickPopupOk(page);
    await page.screenshot({ path: shotPath("QE-4-4", "app", "h-0716-corr-submitted") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
