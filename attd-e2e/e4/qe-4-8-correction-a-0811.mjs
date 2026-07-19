// QE-4-8 step1 — A 8/11 근태보정 상신(대기) IN 10:00 OUT 17:00
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
    // 7월 → 8월 이동
    await page.locator('.mn__btn[aria-label="다음 달"], button[aria-label="다음 달"]').first().click();
    await page.waitForTimeout(2000);
    const hdr = await page.evaluate(() => document.body.innerText.match(/2026[.\-]\s*0?8|8월/)?.[0] || "(월 미확인)");
    console.log("이동 월:", hdr);
    // 11일 셀 클릭
    await page.evaluate(() => {
      const cells = [...document.querySelectorAll("button, td")];
      const el = cells.find((c) => c.innerText && c.innerText.trim().split("\n")[0] === "11" && c.className.includes("cal"));
      if (el) el.click();
    });
    await page.waitForTimeout(2000);
    console.log("11일 상세:", (await page.evaluate(() => document.body.innerText)).slice(-400).replace(/\n/g, " | "));
    await page.locator('button:has-text("수정 요청")').first().click();
    await page.waitForTimeout(1500);
    const sheet = await page.evaluate(() => document.body.innerText.slice(-300).replace(/\n/g, " | "));
    console.log("액션시트:", sheet);
    await page.locator(':text("근태 보정 요청")').first().click();
    await page.waitForTimeout(2500);
    const tsf = page.locator("button.tsf-field");
    const n = await tsf.count();
    console.log("tsf 개수:", n);
    await setStepperTime(page, tsf.nth(0), 10, 0);
    await setStepperTime(page, tsf.nth(1), 17, 0);
    await page.fill("textarea", "[QE-4-8] 8/11 보정 대기(스케줄 교체 판정 테스트)");
    await page.waitForTimeout(600);
    await page.screenshot({ path: shotPath("QE-4-8", "app", "corr-form"), fullPage: true });
    const submit = page.locator('button:has-text("요청하기")').last();
    console.log("요청하기 disabled=", await submit.isDisabled().catch(() => "?"));
    await submit.click();
    await page.waitForTimeout(2500);
    console.log("제출 직후:", (await page.evaluate(() => document.body.innerText)).split("\n").slice(-10).join(" | "));
    await clickPopupOk(page);
    await page.screenshot({ path: shotPath("QE-4-8", "app", "corr-submitted") });
  } catch (e) {
    console.log("FAIL:", e.message);
    process.exitCode = 1;
  } finally {
    await closeAll();
  }
};
main();
